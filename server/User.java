package server;

import common.Config;
import common.Message;
import common.Operation;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.net.*;

/**
 * Classe utile a gestire tutti i dati relativi ad un utente: username, password,
 * documenti che può editare, stato di logIn, eventuali inviti pendenti, documenti
 * in cordo di editing e la coppia <IP, port> associata al client da cui sta facendo
 * il login.
 *
 * @author Lorenzo Beretta, Matricola: 536242
 */
public class User {

    private String username;
    private String password;
    private HashSet<Document> myDocuments;
    private boolean isLogged;
    private boolean wasInvited;
    private Document currentlyEditing;
    private InetSocketAddress clientAddress;

    /**
     * Costruttore
     * @param usr username
     * @param psw password
     */
    User(String usr, String psw) {
        username = usr;
        password = psw;
        myDocuments = new HashSet<>();
        isLogged = false;
        wasInvited = false;
    }

    /**
     * Getter per l'username
     * @return il campo username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Metodo sincronizzato per aggiungere un documento a quelli editabili
     * @param document il dato di tipo Document che descrive il documento
     */
    synchronized void addDocument(Document document) {
        myDocuments.add(document);
    }

    /**
     * Metodo che restituisce la lista dei documenti editabili
     * @return un Vector di ByteBuffer contenenti i bytes delle String dei nomi dei documenti
     */
    synchronized Vector<ByteBuffer> listDocuments() {
        Vector<ByteBuffer> ret = new Vector<>();
        for (Document doc : myDocuments) {
            String docName = doc.getDocumentName();
            ByteBuffer buffer = ByteBuffer.wrap(docName.getBytes());
            ret.add(buffer);
        }
        return ret;
    }

    /**
     * Metodo per effettuare il logIn in caso le credenziali sono giuste
     * @param psw la password tentata dall'Utente
     * @param address la coppia <IP, port> del client
     * @return l'esito del logIn codificato da una Operation
     */
    synchronized Operation logIn(String psw, InetSocketAddress address) {
        if (!password.equals(psw)) return Operation.WRONG_PSW;

        isLogged = true;
        clientAddress = address;
        return Operation.OK;
    }

    /**
     * Metodo per effettuare il logOut che recupera il documento sotto editingUser
     * al fine di permettere la sua regolare chiusura
     * @return il Document che descrive il documento editato da this al momento del logOut
     */
    synchronized Document logOut() {
        isLogged = false;
        Document ret = currentlyEditing;
        currentlyEditing = null;
        clientAddress = null;
        return ret;
    }

    /**
     * Metodo per iniziare l'editing di un Documento (in Docoment è memorizzato il numero della sezione)
     * @param document il document di cui iniziare l'editing
     * @return l'esito della procedura codificato da una Operation
     */
    synchronized Operation startEdit(Document document) {
        if (currentlyEditing != null) return Operation.UNAUTHORIZED;
        currentlyEditing = document;
        return Operation.OK;
    }

    /**
     * Metodo per terminare l'editing del documento currentlyEditing
     * @return l'esito della procedura codificato da una Operation
     */
    synchronized Operation endEdit() {
        if (currentlyEditing == null) return Operation.UNAUTHORIZED;
        currentlyEditing = null;
        return Operation.OK;
    }

    /**
     * Metodo per notificare che this è stato invitato da host ad editare un suo documento
     * @param host User che sta effetuando l'invito
     * @param doc Document al cui editing this guadagna il diritto a prendere parte
     * @param channel il canale UDP su cui mandare le notifiche
     */
    synchronized void sendNotification(User host, Document doc, DatagramChannel channel) {
        if (!isLogged) {
            wasInvited = true;
            return;
        }
        String msg = "(" + host.username + ") ti ha invitato a modificare " + doc.getDocumentName();
        ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
        try {
            channel.send(buffer, clientAddress);
        }
        catch (IOException e) {
            wasInvited = true;
        }
    }

    /**
     * Metodo per notificare all'utente che è stato invitato in sua assenza
     * (portà usare "list" per informarsi sui suoi nuovi privilegi)
     * @param channel il canale UDP su cui mandare le notifiche
     */
    synchronized void sendIfWasInvited(DatagramChannel channel) {
        if (!wasInvited) return;

        wasInvited = false;
        String text =  "(server): sei stato invitato a modificare dei documenti";
        ByteBuffer buffer = ByteBuffer.wrap(text.getBytes());
        try {
            channel.send(buffer, clientAddress);
        }
        catch (IOException e) {
            wasInvited = true;
        }
    }
}

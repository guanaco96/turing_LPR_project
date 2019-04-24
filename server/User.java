package server;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.net.*;

/**
 * -----------------DESCRIZIONE---------------
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
     *
     *
     */
    User(String usr, String psw) {
        username = usr;
        password = psw;
        myDocuments = new HashSet<>();
        isLogged = false;
        wasInvited = false;
    }

    /**
     *
     *
     */
    public String getUsername() {
        return username;
    }

    /**
     *
     *
     */
    synchronized void addDocument(Document document) {
        myDocuments.add(document);
    }

    /**
     *
     *
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
     *
     *
     */
    synchronized Operation logIn(String psw, InetSocketAddress address) {
        if (!password.equals(psw)) return Operation.WRONG_PSW;

        isLogged = true;
        clientAddress = address;
        return Operation.OK;
    }

    /**
     *
     *
     */
    synchronized Document logOut() {
        isLogged = false;
        Document ret = currentlyEditing;
        currentlyEditing = null;
        clientAddress = null;
        return ret;
    }

    /**
     *
     *
     */
    synchronized Operation startEdit(Document document) {
        if (currentlyEditing != null) return Operation.UNAUTHORIZED;
        currentlyEditing = document;
        return Operation.OK;
    }

    /**
     *
     *
     */
    synchronized Operation endEdit() {
        if (currentlyEditing == null) return Operation.UNAUTHORIZED;
        currentlyEditing = null;
        return Operation.OK;
    }

    /**
     *
     *
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
     *
     *
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

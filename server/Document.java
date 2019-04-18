package server;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;

/**
 * -----------------DESCRIZIONE---------------
 *
 * @author Lorenzo Beretta, Matricola: 536242
 */
public class Document {

    private String name;
    private String creator;
    private HashSet<String> invitedUser;

    private int numberOfSections;
    private int sizeOfSection;
    private String[] editingUser;
    Path[] sectionPath;

    /**
     * Costruttore della classe
     * @param documentName nome del documento
     * @param creatorUser username del creatore
     * @param sectionsNumber numero di sezioni
     * @param sizeOfDocument dimensione massima del documento
     */
     Document(String documentName, String creatorUser, int sectionsNumber ,int sizeOfDocument) {
        name = documentName;
        creator = creatorUser;
        numberOfSections = sectionsNumber;
        sizeOfSection = sizeOfDocument / numberOfSections;

        sectionPath = new Path[numberOfSections];
        editingUser = new String[numberOfSections];
        for (String s : editingUser) s = null;

        invitedUser = new HashSet<String>();
        invitedUser.add(creator);
    }

    /**
     * Metodo che utilizza java.nio.FileChannel per leggere una sezione
     * @param section numero della sezione da leggere
     * @return un ByteBuffer contenente il file.
     * @throws IOException se sollevata da FileChannel.read
     */
    private ByteBuffer sectionToBytes(int section) throws IOException {
        FileChannel channel = FileChannel.open(sectionPath[section], StandardOpenOption.READ);
        int size = (int) channel.size();
        ByteBuffer fileBuffer = ByteBuffer.allocate(size);

        while (size > 0) {
            int tmp = channel.read(fileBuffer);
            if (tmp < 0) throw new IOException();
            size -= tmp;
        }

        return fileBuffer;
    }

    /**
     * Metodo che utilizza java.nio.FileChannel per sovrascrivere una sezione
     * @param section numer della sezione da sovrascrivere
     * @param fileBuffer ByteBuffer da scrivere sul nuovo file
     * @throws IOException se sollevata da FileChannel.write
     */
    private void bytesToSection(int section, ByteBuffer fileBuffer) throws IOException {
        FileChannel channel = FileChannel.open(sectionPath[section], StandardOpenOption.TRUNCATE_EXISTING);
        while (fileBuffer.hasRemaining()) {
            channel.write(fileBuffer);
        }
    }

    /**
     * Metodo per iniziare l'edit di una sezione di this
     * @param user username dell'utente che sta tentando di iniziare l'editing
     * @param section numero della sezione da editare
     * @throws IOException se viene sollevata da sectionToBytes
     * @return Message da restituire al client
     */
    synchronized Message startEdit(String user, int section) throws IOException {
        section--; // in questo modo possiamo contare le sezioni a partire da 1

        if (!invitedUser.contains(user)) {
            return new Message(Operation.UNAUTHORIZED);
        }

        if (editingUser[section] != null) return new Message(Operation.SECTION_BUSY);

        editingUser[section] = user;
        return new Message(Operation.OK, sectionToBytes(section));
    }

    /**
     * Metodo per terminare l'editing di una sezine di this
     *
     * @param user username dell'utente che vorrebbe terminare l'editing
     * @param section numero della sezione di cui terminare l'editing
     * @param fileBuffer ByteBuffer in cui è salvata la sezione da sovrascrivere
     * @return  Operation.UNAUTHORIZED  se l'utente non ha i permessi per modificare il documento
                                        oppure se non ha richiesto l'editingUser
                operation.SECTION_SIZE_EXCEEDED se la sezione supera i limiti di spazio
     * @throws IOException se lanciata da bytesToSection
     */
    synchronized Operation endEdit(String user, int section, ByteBuffer fileBuffer) throws IOException {
        section--; // in questo modo possiamo contare le sezioni a partire da 1

        if (!invitedUser.contains(user) || editingUser[section] == null || !editingUser[section].equals(user)) {
            return Operation.UNAUTHORIZED;
        }
        if (fileBuffer.remaining() > sizeOfSection) {
            return Operation.SECTION_SIZE_EXCEEDED;
        }

        bytesToSection(section, fileBuffer);
        editingUser[section] = null;
        return Operation.OK;
    }

    /**
     * Metodo sincronizzato con cui un utente richiede di leggere un documento
     *
     * @param user username dell'utente che richiede il servizio
     * @return Message che contiene una codifica delle sezioni libere e il file del documento
     * @throws IOException sollevata da sectionToBytes
     */
    synchronized Message showDocument(String user) throws IOException {
        if(user == null || !invitedUser.contains(user)) return new Message(Operation.UNAUTHORIZED);
        // per 0 <= i <= numberOfSections il byte i-esimo del corpo del messaggio vale 1 sse la sezione è occupata
        ByteBuffer busySections = ByteBuffer.allocate(numberOfSections);
        for (int i = 0; i < numberOfSections; i++) {
            if (editingUser[i] == null) busySections.putInt(0);
            else busySections.putInt(1);
        }

        Vector<ByteBuffer> documentBuffer = new Vector<ByteBuffer>();
        documentBuffer.add(busySections);

        for (int i = 0; i < numberOfSections; i++) {
            documentBuffer.add(sectionToBytes(i));
        }
        return new Message(Operation.OK, documentBuffer);
    }

    /**
     * Metodo sincronizzato con cui un utente richiede di leggere una sezione di un documento
     *
     * @param user username dell'utente che richiede il servizio
     * @param section la sezione che si vuole mostrata
     * @return Message che contiene una codifica dello stato di editing della sezione e la sezione stessa
     * @throws IOException sollevata da sectionToBytes
     */
    synchronized Message showSection(String user, int section) throws IOException {
        if (user == null || !invitedUser.contains(user)) return new Message(Operation.UNAUTHORIZED);
        // il primo byte del messaggio contiene 0 se la sezione è libera e 1 se è occupata.
        ByteBuffer busySection = ByteBuffer.allocate(1);
        if (editingUser[section] == null) busySection.putInt(0);
        else busySection.putInt(1);

        return new Message(Operation.OK, busySection, sectionToBytes(section));
    }

    /**
     * Metodo che Invita un Utente ad editare un documento
     *
     * @param host utente che invita
     * @param guest utente invitato
     * @return  Operation.UNAUTHORIZED  se host != creator
                Operation.OK            se host == creator
     */
    synchronized Operation invitedUser(String host, String guest) {
        if (!host.equals(creator)) return Operation.UNAUTHORIZED;

        invitedUser.add(guest);
        return Operation.OK;
    }

    // TODO completare il toString se serve....

    /**
     * Overriding del toString che fornisce le informazioni utili al client sul documento
     * il metodo è sincronizzato per garantire la consistenza delle infromazioni stampate
     *
     */
     synchronized public String toString() {
         return "\n>>>>>>>>>>>>>>missing method\n";
     }



}

package server;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;

/**
 * -----------------DESCRIZIONE---------------
 * ACHTUNG! tutte le comparazioni tra users sono fatte by reference
 *
 * @author Lorenzo Beretta, Matricola: 536242
 */
public class Document {

    private String name;
    private User creator;
    private HashSet<User> invitedUser;

    private ChatHandler chatHandler;
    private String chatAddress;

    private int numberOfSections;
    private int sizeOfSection;
    private User[] editingUser;
    private Path[] sectionPath;

    /**
     * Costruttore della classe
     * @param documentName nome del documento
     * @param creatorUser User corrispondente al creatore
     * @param sectionsNumber numero di sezioni
     */
    Document(String documentName, User creatorUser, int sectionsNumber, ChatHandler chat) {
        name = documentName;
        creator = creatorUser;
        numberOfSections = sectionsNumber;
        sizeOfSection = Config.maxDocumentSize / numberOfSections;
        chatHandler = chat;

        sectionPath = new Path[numberOfSections];
        editingUser = new User[numberOfSections];

        invitedUser = new HashSet<User>();
        invitedUser.add(creator);
    }

    /**
     * Getter per il nome del documento
     *
     * @return this.name
     */
    public String getDocumentName() {
        return name;
    }

    /**
     *
     *
     *
     */
    synchronized ByteBuffer getChatAddress(User user) throws IOException {
        if (chatAddress == null) {
            chatAddress = chatHandler.generateAddress();
        }
        return ByteBuffer.wrap(chatAddress.getBytes());
    }

    /**
     *
     *
     *
     */
    Operation createFile() {
        Path path = Paths.get(Config.basePath, creator.getUsername(), name);
        try { Files.createDirectories(path);}
        catch (IOException e) { return Operation.FAIL;}

        for (int i = 1; i <= numberOfSections; i++) {
            try {
                sectionPath[i - 1] = path.resolve("section_" + i);
                Files.createFile(sectionPath[i - 1]);
            }
            catch (FileAlreadyExistsException e) {
                try { Files.delete(sectionPath[i - 1]);}
                catch (IOException exc) { return Operation.FAIL;}
                i--;
            }
            catch (IOException e) { return Operation.FAIL;}
        }
        return Operation.OK;
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
        FileChannel channel = FileChannel.open( sectionPath[section],
                                                StandardOpenOption.TRUNCATE_EXISTING,
                                                StandardOpenOption.WRITE);
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
    synchronized Message startEdit(User user, int section) throws IOException {
        section--; // in questo modo possiamo contare le sezioni a partire da 1

        if (!invitedUser.contains(user)) {
            return new Message(Operation.UNAUTHORIZED);
        }
        if (section >= numberOfSections || section < 0) return new Message(Operation.SECTION_UNKNOWN);
        if (editingUser[section] != null) return new Message(Operation.SECTION_BUSY);

        editingUser[section] = user;
        return new Message(Operation.OK, sectionToBytes(section), getChatAddress(user));
    }

    /**
     * Metodo per terminare l'editing di una sezine di this
     *
     * @param user User relativo all'utente che vorrebbe terminare l'editing
     * @param section numero della sezione di cui terminare l'editing
     * @param fileBuffer ByteBuffer in cui è salvata la sezione da sovrascrivere
     * @return  Operation.UNAUTHORIZED  se l'utente non ha i permessi per modificare il documento
                                        oppure se non ha richiesto l'editingUser
                operation.SECTION_SIZE_EXCEEDED se la sezione supera i limiti di spazio
     * @throws IOException se lanciata da bytesToSection
     */
    synchronized Operation endEdit(User user, int section, ByteBuffer fileBuffer) throws IOException {
        section--; // in questo modo possiamo contare le sezioni a partire da 1

        if (!invitedUser.contains(user) || editingUser[section] == null || editingUser[section] != user) {
            return Operation.UNAUTHORIZED;
        }
        if (fileBuffer.remaining() > sizeOfSection) {
            return Operation.SECTION_SIZE_EXCEEDED;
        }

        bytesToSection(section, fileBuffer);
        editingUser[section] = null;
        freeIfUseless();
        return Operation.OK;
    }

    /**
     * Metodo sincronizzato con cui un utente richiede di leggere un documento
     *
     * @param user username dell'utente che richiede il servizio
     * @return Message che contiene una codifica delle sezioni libere e il file del documento
     * @throws IOException sollevata da sectionToBytes
     */
    synchronized Message showDocument(User user) throws IOException {
        if(user == null || !invitedUser.contains(user)) return new Message(Operation.UNAUTHORIZED);
        // per 0 <= i <= numberOfSections il byte i-esimo del corpo del messaggio vale 1 sse la sezione è occupata
        ByteBuffer busySections = ByteBuffer.allocate(4 * numberOfSections);
        for (int i = 0; i < numberOfSections; i++) {
            if (editingUser[i] == null) busySections.putInt(0);
            else busySections.putInt(1);
        }
        busySections.flip();

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
    synchronized Message showSection(User user, int section) throws IOException {
        section--;

        if (user == null || !invitedUser.contains(user)) return new Message(Operation.UNAUTHORIZED);
        if (section >= numberOfSections || section < 0) return new Message(Operation.SECTION_SIZE_EXCEEDED);
        // il primo byte del messaggio contiene 0 se la sezione è libera e 1 se è occupata.
        ByteBuffer busySection = ByteBuffer.allocate(4);
        if (editingUser[section] == null) busySection.putInt(0);
        else busySection.putInt(1);
        busySection.flip();

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
    synchronized Operation inviteUser(User host, User guest) {
        if (host != creator) return Operation.UNAUTHORIZED;

        invitedUser.add(guest);
        return Operation.OK;
    }

    /**
     *
     *
     *
     */
    void freeIfUseless() {
        int sec = 0;
        while (sec < numberOfSections && editingUser[sec] == null) sec++;
        if (sec == numberOfSections) {
            chatHandler.freeAddress(chatAddress);
            chatAddress = null;
        }
    }

    void logOut(User user) {
        for (int i = 0; i < numberOfSections; i++) {
            if (editingUser[i] == user) editingUser[i] = null;
        }
        freeIfUseless();
    }

}

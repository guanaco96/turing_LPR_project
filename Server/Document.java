package Server;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.channels.*;
import java.util.*;

/**
 * -----------------DESCRIZIONE---------------
 *
 * @author Lorenzo Beretta, Matricola: 536242
 */
public class Document {

    private String name;
    private String creator;
    private Set<String> invitedUser;

    private int numberOfSections;
    private int sizeOfSection;
    private String[] editingUser;
    private Path[] sectionPath;

    /**
     * Costruttore package-private
     * @param documentName nome del documento
     * @param creatorUser username del creatore
     * @param sectionsNumber numero di sezioni
     * @param sizeOfDocument dimensione massima del documento
     */
     Document(String documentName,String creatorUser, int sectionsNumber ,int sizeOfDocument) {
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
     * Metodo pubblico per creare il corrispettivo del documento nel file system
     * @param directoryPath directory in cui salvare "creator/name/sezione_i"
     * @throws IOException sollevata da Files.createFile e Files.delete
     */
    public void createFiles(String directoryPath) throws IOException {
        Path path = Paths.get(directoryPath, creator, name);
        Files.createDirectories(path);
        for (int i = 0; i < numberOfSections; i++) {
            try {
                sectionPath[i] = path.resolve("section_" + i);
                Files.createFile(sectionPath[i]);
            }
            catch (FileAlreadyExistsException e) {
                Files.delete(sectionPath[i]);
                i--;
            }
        }
    }

    /**
     * Metodo statico che utilizza java.nio.FileChannel per leggere un file
     * @param path path del file da leggere
     * @return un ByteBuffer contenente il file.
     * @throws IOException if an error occurs during I/O operation on the file
     */
    private static ByteBuffer fileToBytes(Path path) throws IOException {
        FileChannel channel = FileChannel.open(path,StandardOpenOption.READ);
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
     * Metodo per iniziare l'edit di una sezione di this
     * @param user username dell'utente che sta tentando di iniziare l'editing
     * @param section numero della sezione da editare
     * @throws IOException se viene sollevata da fileToBytes
     * @return Message da restituire al client
     */
    synchronized Message startEdit(String user, int section) throws IOException {
        section--; // in questo modo possiamo contare le sezioni a partire da 1

        if (user == null || !invitedUser.contains(user)) {
            return new Message(Operation.UNAUTHORIZED);
        }

        if (editingUser[section] != null) return new Message(Operation.SECTION_BUSY);

        editingUser[section] = user;
        return new Message(Operation.OK, fileToBytes(sectionPath[section]));
    }

    /**
     *
     * @param
     * @return
     * @throws IOException
     */
    synchronized Message showDocument(String user) throws IOException {
        if(user == null || !invitedUser.contains(user)) return new Message(Operation.UNAUTHORIZED);
        // invio un messaggio al client con l'elenco dei numeri delle sezioni occupate
        ByteBuffer busySections = ByteBuffer.allocate(numberOfSections * 4);
        for (int i = 0; i < numberOfSections; i++) {
            if (editingUser[i] == null) continue;
            busySections.putInt(i);
        }

        Vector<ByteBuffer> documentBuffer = new Vector<ByteBuffer>();
        documentBuffer.add(busySections);

        for (int i = 0; i < numberOfSections; i++) {
            documentBuffer.add(fileToBytes(sectionPath[i]));
        }

        return new Message(Operation.OK, documentBuffer);
    }

}

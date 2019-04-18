
// ------------------------- DEPRECATED --------------------------

package server;

import java.util.concurrent.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;


/**
 *
 *
 */
public class Shelf {

    private ConcurrentHashMap<String,Document> map;
    private final String basePath;
    private final int sizeOfDocument;

    /**
     *
     *
     */
    Shelf(String path, int documentSize) {
        map = new ConcurrentHashMap<>();
        basePath = path;
        sizeOfDocument = documentSize;
    }

    /**
     * Metodo per creare il documento come oggetto e nel file system
     *
     *
     */
    Document createDocument (String name, String creator, int numberOfSections) throws IOException {
        // crea l'oggetto di classe Document
        Document document = new Document(name, creator, numberOfSections, sizeOfDocument);
        // crea i files delle sezioni nel file system
        Path path = Paths.get(basePath, creator, name);
        Files.createDirectories(path);
        for (int i = 0; i < numberOfSections; i++) {
            try {
                document.sectionPath[i] = path.resolve("section_" + i);
                Files.createFile(document.sectionPath[i]);
            }
            catch (FileAlreadyExistsException e) {
                Files.delete(document.sectionPath[i]);
                i--;
            }
        }
        // inserisce il documento nell'hashMap
        map.put(path.toString(), document);
        return document;
    }

    /**
     *
     *
     */
    Document getDocument(String documentPath) {
        return map.get(documentPath);
    }

    /**
     *
     *
     */


     /**
      *
      *
      */



      /**
       *
       *
       */

}

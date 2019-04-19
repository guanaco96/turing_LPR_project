package server;

// TODO nella funzione di creazione di un documento devo implementare la crezione nel
// file system che si trova in DEPRECATED shelf
/*
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
*/

import java.util.*;
import java.util.concurrent.*;
import java.nio.channels.*;
import java.net.*;

public class TaskExecutor implements Runnable {
    private ConcurrentHashMap<String,User> userMap;
    private ConcurrentHashMap<String,Document> documentMap;
    private ConcurrentHashMap<SocketChannel,User> socketMap;
    private BlockingQueue<SocketChannel> queue;
    private Selector selector;
    private DatagramChannel datagramChannel;
    private ChatHandler chatHandler;
    private SocketChannel socketChannel;

    /**
     * Costruttore vuoto
     */
    TaskExecutor() {}

    /**
     *
     *
     */
    public void run() {
        Message request = Message.read(socketChannel);
        Message reply = satisfy(request);


    }
    /**
     *
     *
     */
    Message satisfy(Message request) {
        Message reply;
        Vector<byte[]> chunks = request.segment();
        User user = socketMap.get(socketChannel);
        if (request.getOp() != Operation.LOGIN && user != null) return new Message(Operation.NOT_LOGGED);

        switch (request.getOp()) {

            case Operation.LOGIN:
                if (chunks.size() != 3) return new Message(Operation.WRONG_REQUEST);
                if (user != null) return new Message(Operation.ALREADY_LOGGED);

                user = userMap.get(String(chunks.get(0)));
                String psw = String(chunks.get(1));
                int port = ByteBuffer.wrap(chunks.get(2)).getInt();

                InetSocketAddress rsa = socketChannel.getRemoteAddress();
                InetAddress ra = rsa.getAddress();
                InetSocketAddress udpsa = InetSocketAddress(ra, port);

                Operation responseOp = user.logIn(psw, udpsa);
                if (responseOp == Operation.OK) socketMap.add(socketChannel, user);
                return new Message(responseOp);
                break;

            case Operation.create:
                if (chunks.size() != 2) return new Message(Operation.WRONG_REQUEST);

                String documentName = String(chunks.get(0));
                int numberOfSections = ByteBuffer.wrap(chunks.get(1)).getInt();
                //Document doc = new Document(s


            // FAKE
            default: return new Message();

        }

    }

    /**
     *
     *
     */

    /**
     *
     *
     */

}

package server;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.nio.*;
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
        try {
            Message request = Message.read(socketChannel);
            Message reply = satisfy(request);
        }
        catch (IOException e) {
            Message reply = new Message(Operation.FAIL);
        }

    }

    /**
     *
     *
     */
    Message satisfy(Message request) throws IOException {
        Vector<byte[]> chunks = request.segment();
        User user = socketMap.get(socketChannel);
        if (request.getOp() != Operation.LOGIN && user == null) return new Message(Operation.NOT_LOGGED);

        switch (request.getOp()) {

            case LOGIN:
                if (chunks.size() != 3) return new Message(Operation.WRONG_REQUEST);
                if (user != null) return new Message(Operation.ALREADY_LOGGED);
                user = userMap.get(new String(chunks.get(0)));
                String psw = new String(chunks.get(1));
                int port = ByteBuffer.wrap(chunks.get(2)).getInt();

                InetSocketAddress rsa = (InetSocketAddress) socketChannel.getRemoteAddress();
                InetSocketAddress udpsa = new InetSocketAddress(rsa.getAddress(), port);
                Operation reply = user.logIn(psw, udpsa);
                if (reply == Operation.OK) socketMap.put(socketChannel, user);
                return new Message(reply);

            case CREATE:
                if (chunks.size() != 2) return new Message(Operation.WRONG_REQUEST);
                String documentName = new String(chunks.get(0));
                int numberOfSections = ByteBuffer.wrap(chunks.get(1)).getInt();

                Document document = new Document(documentName, user, numberOfSections, chatHandler);
                if (documentMap.putIfAbsent(documentName, document) != null) {
                    return new Message(Operation.DUPLICATE_DOCUMENT);
                }
                Operation reply = document.createFile();
                if (reply == Operation.OK) creator.addDocument(document);
                return new Message(reply);

            case INVITE:
                if (chunks.size() != 2) return new Message(Operation.WRONG_REQUEST);
                String documentName = new String(chunks.get(0));
                String guestName = new String(chunks.get(1));
                User guest = userMap.get(guestName);
                Document document = documentMap.get(documentName);

                if (guest == null) return new Message(Operation.USER_UNKNOWN);
                if (document == null) return new Message(Operation.DOCUMENT_UNKNOWN);
                Operation reply = document.inviteUser(user, guest);
                if (reply == Operation.OK) {
                    guest.addDocument(document);
                    guest.sendNotification(user, document, datagramChannel);
                }
                return new Message(reply);

            case LIST:
                if (!chunks.isEmpty()) return new Message(Operation.WRONG_REQUEST);
                Vector<ByteBuffer> docList = user.listDocuments();
                return Message(Operation.OK, docList);

            case SHOW_SECTION:
                if (chunks.size() != 2) return new Message(Operation.WRONG_REQUEST);
                String documentName = new String(chunks.get(0));
                int section = ByteBuffer.wrap(chunks.get(1)).getInt();
                Document document = documentMap.get(documentName);

                if (document == null) return new Message(Operation.DOCUMENT_UNKNOWN);
                return document.showSection(user, section);

            case SHOW_DOCUMENT:
                if (chunks.size() != 1) return new Message(Operation.WRONG_REQUEST);
                String documentName = new String(chunks.get(0));

                Document document = documentMap.get(documentName);

                if (document == null) return new Message(Operation.DOCUMENT_UNKNOWN);
                return document.showDocument(user);

            case START_EDIT:
                if (chunks.size() != 2) return new Message(Operation.WRONG_REQUEST);
                String documentName = new String(chunks.get(0));
                int section = ByteBuffer.wrap(chunks.get(1)).getInt();
                Document document = documentMap.get(documentName);

                Operation reply = user.startEdit(document);
                if (reply != Operation.OK) return new Message(reply);
                try {
                    reply = document.startEdit(user, section);
                }
                catch (IOException e) {
                    user.endEdit();
                }
                if (reply != Operation.OK) user.endEdit();
                return new Message(reply);

            case END_EDIT:
                


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

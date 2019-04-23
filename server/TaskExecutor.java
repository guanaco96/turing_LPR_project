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
     * Costruttore
     */
    public TaskExecutor(   ConcurrentHashMap<String,User> usrMp,
                    ConcurrentHashMap<String,Document> docMp,
                    ConcurrentHashMap<SocketChannel,User> sckMp,
                    BlockingQueue<SocketChannel> q,
                    Selector sel,
                    DatagramChannel datagram,
                    ChatHandler chat,
                    SocketChannel socket) {

        userMap = usrMp;
        documentMap = docMp;
        socketMap = sckMp;
        queue = q;
        selector = sel;
        datagramChannel = datagram;
        chatHandler = chat;
        socketChannel = socket;
    }

    /**
     *
     *
     */
    private void logOut() {
        User user = socketMap.remove(socketChannel);
        if (user != null) {
            Document document = user.logOut();
            if (document != null) document.logOut(user);
        }
    }

    /**
     *
     *
     */
    Message satisfy(Message request) throws IOException {
        Vector<byte[]> chunks = request.segment();
        User user = socketMap.get(socketChannel);

        // TODO
        // System.out.println("request.getOp() = " + request.getOp() + "\nuser = " + user);

        if (request.getOp() != Operation.LOGIN && user == null) return new Message(Operation.NOT_LOGGED);

        switch (request.getOp()) {

            case LOGIN: {
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
            }
            case CREATE: {
                if (chunks.size() != 2) return new Message(Operation.WRONG_REQUEST);
                String documentName = new String(chunks.get(0));
                int numberOfSections = ByteBuffer.wrap(chunks.get(1)).getInt();

                Document document = new Document(documentName, user, numberOfSections, chatHandler);
                if (documentMap.putIfAbsent(documentName, document) != null) {
                    return new Message(Operation.DUPLICATE_DOCUMENT);
                }
                Operation reply = document.createFile();
                if (reply == Operation.OK) user.addDocument(document);
                return new Message(reply);
            }
            case INVITE: {
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
            }
            case LIST: {
                if (!chunks.isEmpty()) return new Message(Operation.WRONG_REQUEST);
                Vector<ByteBuffer> docList = user.listDocuments();
                return new Message(Operation.OK, docList);
            }
            case SHOW_SECTION: {
                if (chunks.size() != 2) return new Message(Operation.WRONG_REQUEST);
                String documentName = new String(chunks.get(0));
                int section = ByteBuffer.wrap(chunks.get(1)).getInt();
                Document document = documentMap.get(documentName);

                if (document == null) return new Message(Operation.DOCUMENT_UNKNOWN);
                return document.showSection(user, section);
            }
            case SHOW_DOCUMENT: {
                if (chunks.size() != 1) return new Message(Operation.WRONG_REQUEST);
                String documentName = new String(chunks.get(0));

                Document document = documentMap.get(documentName);

                if (document == null) return new Message(Operation.DOCUMENT_UNKNOWN);
                return document.showDocument(user);
            }
            case START_EDIT: {
                if (chunks.size() != 2) return new Message(Operation.WRONG_REQUEST);
                String documentName = new String(chunks.get(0));
                int section = ByteBuffer.wrap(chunks.get(1)).getInt();
                Document document = documentMap.get(documentName);
                if (document == null) return new Message(Operation.DOCUMENT_UNKNOWN);

                Operation reply = user.startEdit(document);
                if (reply != Operation.OK) return new Message(reply);
                Message msg = null;
                try {
                    msg = document.startEdit(user, section);
                }
                catch (IOException e) {
                    user.endEdit();
                }
                if (msg != null && msg.getOp() != Operation.OK) user.endEdit();
                return new Message(reply);
            }
            case END_EDIT: {
                if (chunks.size() != 3) return new Message(Operation.WRONG_REQUEST);
                String documentName = new String(chunks.get(0));
                int section = ByteBuffer.wrap(chunks.get(1)).getInt();
                ByteBuffer fileBuffer = ByteBuffer.wrap(chunks.get(2));
                Document document = documentMap.get(documentName);

                Operation reply = user.endEdit();
                if (reply != Operation.OK) return new Message(reply);
                reply = document.endEdit(user, section, fileBuffer);
                return new Message(reply);
            }
            case LOGOUT: {
                logOut();
                return new Message(Operation.OK);
            }
            default: return new Message(Operation.WRONG_REQUEST);
        }
    }

    /**
     *
     *
     */
    public void run() {
        Message request = null;
        Message reply = null;
        try {
            request = Message.read(socketChannel);
            reply = satisfy(request);
            reply.write(socketChannel);

            // TODO
            // System.out.println("reply.getOp() = " + reply.getOp());

            if (request.getOp() == Operation.LOGOUT) socketChannel.close();
            else queue.put(socketChannel);
        }
        catch (IOException | InterruptedException e) {

            // TODO
            // e.printStackTrace();

            logOut();
            try {
                socketChannel.close();
            } catch (IOException exc) {}
        }

        if (request != null && request.getOp() == Operation.LOGIN) {
            User user = socketMap.get(socketChannel);
            if (user != null) user.sendIfWasInvited(datagramChannel);
        }
        selector.wakeup();
    }
}

package server;

import common.Config;
import common.Message;
import common.Operation;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;

/**
 * Classe che implementa Runnable, qui è dichiarato il mai dei thread worker che
 * eseguiranno le richieste dei clients.
 *
 * @author Lorenzo Beretta, Matricola: 536242
 */
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
     * @param usrMp mappa i nomi degli utenti nell'oggetto User che li descrive
     * @param docMp mappa i nomi dei documenti nell'oggetto Document che li descrive
     * @param sckMp mappa i SocketChannel dei clients agli utenti su essi loggati
     * @param q la coda di SocketChannel per raccogliere i clients non disconnessi dopo essere stati serviti
     * @param sel selettore
     * @param datagram canale UDP per l'invio delle notifiche
     * @param chat istanza di ChatHandler per la gestione degli indirizzi multicast
     * @param socket socket a cui è connesso il client
     */
    public TaskExecutor(ConcurrentHashMap<String,User> usrMp,
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
     * Metodo che coordina il logout dell'utente connesso tramite il
     * client sfruttando i metodi delle classi User e Document
     */
    private void logOut() {
        User user = socketMap.remove(socketChannel);
        if (user != null) {
            Document document = user.logOut();
            if (document != null) document.logOut(user);
        }
    }

    /**
     * Metodo che suddivide nelle porzioni originali la request del client,
     * distingue il tipo di operazione da effettuare e coordina i metodi delle
     * classi User e Document, tenendo aggiornate le tabelle socketMap, userMap
     * e documentMap.
     * @param request la richiesta spedita dal client
     * @return il Messaggio da spedire in risposta al client
     * @throws IOException risolleva l'eccezione sollevata da molti dei metodi definiti in User e Document
     */
    Message satisfy(Message request) throws IOException {
        Vector<byte[]> chunks = request.segment();
        User user = socketMap.get(socketChannel);

        // caso in cui l'utente non ha ancora effettuato il login e richiede un'altra operazione
        if (request.getOp() != Operation.LOGIN && user == null) return new Message(Operation.NOT_LOGGED);

        switch (request.getOp()) {
            // ------------------------- LOGIN -------------------------------
            case LOGIN: {
                if (chunks.size() != 3) return new Message(Operation.WRONG_REQUEST);
                if (user != null) return new Message(Operation.ALREADY_LOGGED);
                user = userMap.get(new String(chunks.get(0)));
                String psw = new String(chunks.get(1));
                int port = ByteBuffer.wrap(chunks.get(2)).getInt();

                InetSocketAddress rsa = (InetSocketAddress) socketChannel.getRemoteAddress();
                InetSocketAddress udpsa = new InetSocketAddress(rsa.getAddress(), port);
                if (user == null) return new Message(Operation.USER_UNKNOWN);
                Operation reply = user.logIn(psw, udpsa);
                if (reply == Operation.OK) socketMap.put(socketChannel, user);
                return new Message(reply);
            }
            // ------------------------- CREATE -------------------------------
            case CREATE: {
                if (chunks.size() != 2) return new Message(Operation.WRONG_REQUEST);
                String documentName = new String(chunks.get(0));
                int numberOfSections = ByteBuffer.wrap(chunks.get(1)).getInt();
                if(numberOfSections <= 0) return new Message(Operation.WRONG_REQUEST);

                Document document = new Document(documentName, user, numberOfSections, chatHandler);
                if (documentMap.putIfAbsent(documentName, document) != null) {
                    return new Message(Operation.DUPLICATE_DOCUMENT);
                }
                Operation reply = document.createFile();
                if (reply == Operation.OK) user.addDocument(document);
                return new Message(reply);
            }
            // ------------------------- INVITE -------------------------------
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
            // ------------------------- LIST -------------------------------
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
            // ------------------------- SHOW_DOCUMENT -------------------------------
            case SHOW_DOCUMENT: {
                if (chunks.size() != 1) return new Message(Operation.WRONG_REQUEST);
                String documentName = new String(chunks.get(0));

                Document document = documentMap.get(documentName);

                if (document == null) return new Message(Operation.DOCUMENT_UNKNOWN);
                return document.showDocument(user);
            }
            // ------------------------- START_EDIT -------------------------------
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
                    return new Message(Operation.FAIL);
                }
                if (msg.getOp() != Operation.OK) user.endEdit();
                return msg;
            }
            // ------------------------- END_EDIT -------------------------------
            case END_EDIT: {
                if (chunks.size() != 3) return new Message(Operation.WRONG_REQUEST);
                String documentName = new String(chunks.get(0));
                int section = ByteBuffer.wrap(chunks.get(1)).getInt();
                ByteBuffer fileBuffer = ByteBuffer.wrap(chunks.get(2));
                Document document = documentMap.get(documentName);

                Operation reply = user.endEdit();
                if (reply != Operation.OK) return new Message(reply);
                reply = document.endEdit(user, section, fileBuffer);
                if (reply != Operation.OK) user.startEdit(document);
                return new Message(reply);
            }
            // ------------------------- LOGOUT -------------------------------
            case LOGOUT: {
                logOut();
                return new Message(Operation.OK);
            }
            // -------------------- C'E' STATO UN ERRORE -----------------------
            default: return new Message(Operation.WRONG_REQUEST);
        }
    }

    /**
     * Metodo di Runnable implementato.
     * Il thread legge la richista del client, la soddisfa ed invia la risposta
     * curandosi di trattare in modo particolare i casi di disconnessione e riconnessione.
     */
    public void run() {
        Message request = null;
        Message reply = null;
        try {
            request = Message.read(socketChannel);
            reply = satisfy(request);
            reply.write(socketChannel);

            if (request.getOp() == Operation.LOGOUT && reply.getOp() == Operation.OK) {
                socketChannel.close();
            } else queue.put(socketChannel);
        }
        catch (IOException | InterruptedException e) {
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

package server;

// TODO nella funzione di creazione di un documento devo implementare la crezione nel
// file system che si trova in DEPRECATED shelf

import java.util.*;
import java.util.concurrent.*;
import java.nio.channels.*;
import java.net.*;

public class WorkerTask implements Runnable {
    private ConcurrentHashMap<String,User> userMap;
    private ConcurrentHashMap<String,Document> documentMap;
    private ConcurrentHashMap<SocketChannel,User> socketMap;
    private BlockingQueue<SocketChannel> queue;
    private Selector selector;
    private DatagramChannel datagramChannel;
    private ChatAddressHandler chatHandler;
    private SocketChannel socketChannel;

    /**
     * Costruttore vuoto
     */
    ServerExecutor() {}

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
                Document doc = new Document(




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

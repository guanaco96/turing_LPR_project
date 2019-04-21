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
public class Server {

    private ConcurrentHashMap<String,User> userMap;
    private ConcurrentHashMap<String,Document> documentMap;
    private ConcurrentHashMap<SocketChannel,User> socketMap;
    private BlockingQueue<SocketChannel> queue;
    private Selector selector;
    private ThreadPoolExecutor threadPool;
    private DatagramChannel datagramChannel;
    private ServerSocketChannel wellcome;
    private ChatHandler chatHandler;

    /**
     *
     *
     */
    public static void main(String[] args) {
        queue = new LinkedBlockingDeque<>();
        datagramChannel = DatagramChannel.open();
        datagramChannel.socket().bind(new InetSocketAddress(Config.portUDP));
        

    }

}

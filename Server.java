import server.User;
import server.Document;
import server.ChatHandler;
import server.Config;
import server.TaskExecutor;
import server.RemoteTableImplementation;
import remote.RemoteTableInterface;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.rmi.server.*;
import java.rmi.registry.*;

/**
 * -----------------DESCRIZIONE---------------
 *
 * @author Lorenzo Beretta, Matricola: 536242
 */
public class Server {

    private static ConcurrentHashMap<String,User> userMap;
    private static ConcurrentHashMap<String,Document> documentMap;
    private static ConcurrentHashMap<SocketChannel,User> socketMap;
    private static BlockingQueue<SocketChannel> queue;
    private static Selector selector;
    private static ThreadPoolExecutor threadPool;
    private static DatagramChannel datagramChannel;
    private static ServerSocketChannel welcome;
    private static ChatHandler chatHandler;

    /**
     *
     *
     */
    public static void main(String[] args) {

        // inizializzo le strutture dati
        userMap = new ConcurrentHashMap<>();
        documentMap = new ConcurrentHashMap<>();
        socketMap = new ConcurrentHashMap<>();
        queue = new LinkedBlockingDeque<>();
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(Config.threadsInPool);
        chatHandler = new ChatHandler(Config.base, Config.bound, Config.portChat);

        try {
            // attivo il servizio di registrazione RMI
            RemoteTableImplementation remoteTable = new RemoteTableImplementation(userMap);
            RemoteTableInterface stub = (RemoteTableInterface) UnicastRemoteObject.exportObject(remoteTable,0);
            LocateRegistry.createRegistry(Config.portRegistryRMI);
            Registry registry = LocateRegistry.getRegistry(Config.portRegistryRMI);
            registry.rebind("REGISTER-TURING",stub);

            // configuro i canali e registro il ServerSocketChannel welcome nel selector
            datagramChannel = DatagramChannel.open();
            datagramChannel.socket().bind(new InetSocketAddress(Config.portUDP));
            welcome = ServerSocketChannel.open();
            welcome.bind(new InetSocketAddress(Config.portTCP));
            welcome.configureBlocking(false);
            selector = Selector.open();
            welcome.register(selector, SelectionKey.OP_ACCEPT);


            // ciclo di esecuzione del server
            while (!Thread.interrupted()) {

                // registro i socket in coda
                Vector<SocketChannel> tmpQueue = new Vector<>();
                queue.drainTo(tmpQueue);
                for (SocketChannel socket : tmpQueue) {
                    socket.register(selector, SelectionKey.OP_READ);
                }

                // selezione dei canali pronti per la lettura
                selector.select(Config.selectorTime);
                Set<SelectionKey> keySet = selector.selectedKeys();

                for (Iterator<SelectionKey> it = keySet.iterator(); it.hasNext(); it.remove()) {
                    SelectionKey key = it.next();
                    if (key.isAcceptable()) {
                        SocketChannel newSocketChannel = welcome.accept();
                        newSocketChannel.configureBlocking(false);
                        newSocketChannel.register(selector, SelectionKey.OP_READ);
                    }
                    if (key.isReadable()) {
                        SocketChannel clientSocket = (SocketChannel) key.channel();
                        key.cancel();

                        TaskExecutor task = new TaskExecutor(userMap, documentMap, socketMap, queue, selector,
                                                                datagramChannel, chatHandler, clientSocket);
                        threadPool.execute(task);
                    }
                }

            }
        }
        catch (IOException e) {
            System.out.println("Errore del server!");
        }
    }
}

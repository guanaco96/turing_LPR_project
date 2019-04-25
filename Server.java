import server.User;
import server.Document;
import server.ChatHandler;
import server.TaskExecutor;
import server.RemoteTableImplementation;
import common.Config;
import common.RemoteTableInterface;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;

/**
 * Questa classe contiene il Main del server TURING.
 * Nel main viene esportato lo stub per RMI, vengono aperti ed inizializzati il selettore, il socket
 * di welcome per i client e i channel UDP per le notifiche. Viene infine creato un threadPool.
 * Il main entra poi nel suo ciclo principale: registra tutti i socket di client già serviti ma che non si sono
 * ancora disconnessi presenti su una coda, seleziona quelli pronti per operazioni di I/O e li fa servire dai threads
 * del pool tramite dei Runnable (TaskExecutor).
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
     * Main del server TURING
     */
    public static void main(String[] args) {

        // inizializzo le strutture dati
        userMap = new ConcurrentHashMap<>();
        documentMap = new ConcurrentHashMap<>();
        socketMap = new ConcurrentHashMap<>();
        queue = new LinkedBlockingDeque<>();
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(Config.threadsInPool);
        chatHandler = new ChatHandler();

        // attivo il servizio di registrazione RMI
        RemoteTableImplementation remoteTable = new RemoteTableImplementation(userMap);
        try {
            RemoteTableInterface stub = (RemoteTableInterface) UnicastRemoteObject.exportObject(remoteTable,0);
            LocateRegistry.createRegistry(Config.portRegistryRMI);
            Registry registry = LocateRegistry.getRegistry(Config.portRegistryRMI);
            registry.rebind("REGISTER-TURING", stub);
        }
        catch (RemoteException e) {
            System.out.println("Errore nella creazione dell'oggetto remoto");
            System.exit(-1);
        }

        // configuro i canali e registro il ServerSocketChannel welcome nel selector
        try {
            datagramChannel = DatagramChannel.open();
            datagramChannel.socket().bind(new InetSocketAddress(Config.portUDP));
            welcome = ServerSocketChannel.open();
            welcome.bind(new InetSocketAddress(Config.portTCP));
            welcome.configureBlocking(false);
            selector = Selector.open();
            welcome.register(selector, SelectionKey.OP_ACCEPT);
        }
        catch (IOException e) {
            System.out.println("Errore nell'inizializzazione dei socket");
            System.exit(-1);
        }

        // ciclo di esecuzione del server
        while (!Thread.interrupted()) {

            // registro i socket in coda
            Vector<SocketChannel> tmpQueue = new Vector<>();
            queue.drainTo(tmpQueue);
            for (SocketChannel socket : tmpQueue) {
                try { socket.register(selector, SelectionKey.OP_READ);}
                catch (ClosedChannelException e) {
                    socketMap.remove(socket);
                }
            }

            // selezione dei canali pronti per la lettura
            try { selector.select(Config.selectorTime);}
            catch (IOException e) { e.printStackTrace();}

            Set<SelectionKey> keySet = selector.selectedKeys();

            for (Iterator<SelectionKey> it = keySet.iterator(); it.hasNext(); it.remove()) {
                SelectionKey key = it.next();
                if (key.isAcceptable()) {
                    SocketChannel newSocketChannel = null;
                    try {
                        newSocketChannel = welcome.accept();
                        newSocketChannel.configureBlocking(false);
                        newSocketChannel.register(selector, SelectionKey.OP_READ);
                    }
                    catch (IOException e) {
                        socketMap.remove(newSocketChannel);
                    }
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
}

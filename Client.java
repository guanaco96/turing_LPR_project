import common.RemoteTableInterface;
import common.Config;
import common.Message;
import common.Operation;
import client.ChatListener;
import client.Notifier;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.net.*;
import java.util.*;


/**
 * Classe contenente il main del client.
 * Data la semplicità della CLI essa è implementata nel main stesso.
 *
 * @author Lorenzo Beretta, Matricola: 536242
 */
 public class Client {

    static String loggedUser;
    static ChatListener chatListener;
    static Notifier notifier;
    static Thread listener;
    static Thread notifierThread;
    static InetAddress chatAddress;
    static SocketChannel socket;
    static String serverName;

    /**
     * Main del client: prende come argomento l'indirizzo del Server se è remoto.
     * Stampa il prompt, tokenizza l'imput, in base al [COMMAND] inserito chiama
     * l'opportuno metodo statico o stampa un messaggio di errore se il comando
     * è mal formattato.
     * @param args se args[0] è presente questo è l'indirizzo del server remoto
     */
    public static void main(String[] args) {
        serverName = (args.length > 0) ? args[0] : null;
        connectSocket();
        System.out.println("Benvenuto in TURING, scrivi \"help\" per l'elenco dei comandi");

        while (!Thread.interrupted()) {
            Vector<String> token = new Vector<>();

            System.out.printf("\n[turing] >> ");
            Scanner scanner = new Scanner(System.in);
            String line = scanner.nextLine();
            StringTokenizer tokenizer = new StringTokenizer(line);
            while (tokenizer.hasMoreTokens()) {
                token.add(tokenizer.nextToken());
            }
            if (token.isEmpty()) {
                wrongCommand();
                continue;
            }

            switch (token.get(0)) {
                case "help":
                    printHelpMessage();
                    break;

                case "register":
                    if (token.size() == 3) {
                        String usr = token.get(1);
                        String psw = token.get(2);
                        register(usr, psw);
                    }
                    else wrongCommand();
                    break;

                case "login":
                    if (token.size() == 3) {
                        String usr = token.get(1);
                        String psw = token.get(2);
                        login(usr, psw);
                    }
                    else wrongCommand();
                    break;

                case "logout":
                    if (token.size() == 1) logout();
                    else wrongCommand();
                    break;

                case "create":
                    if (token.size() == 3) {
                        String docName = token.get(1);
                        int secNum = Integer.parseInt(token.get(2));
                        create(docName, secNum);
                    }
                    else wrongCommand();
                    break;

                case "share":
                    if (token.size() == 3) {
                        String docName = token.get(1);
                        String guest = token.get(2);
                        share(docName, guest);
                    }
                    else wrongCommand();
                    break;

                case "show":
                    if (token.size() == 3) {
                        String docName = token.get(1);
                        int numSec = Integer.parseInt(token.get(2));
                        showSection(docName, numSec);
                    }
                    else if (token.size() == 2) {
                        String docName = token.get(1);
                        showDocument(docName);
                    }
                    else wrongCommand();
                    break;

                case "list":
                    if (token.size() == 1) list();
                    else wrongCommand();
                    break;

                case "edit":
                    if (token.size() == 3) {
                        String docName = token.get(1);
                        int numSec = Integer.parseInt(token.get(2));
                        edit(docName, numSec);
                    }
                    else wrongCommand();
                    break;

                case "end-edit":
                    if (token.size() == 3) {
                        String docName = token.get(1);
                        int numSec = Integer.parseInt(token.get(2));
                        endEdit(docName, numSec);
                    }
                    else wrongCommand();
                    break;

                case "send":
                    if (token.size() >= 2) {
                        String text = "(" + loggedUser + "): ";
                        text = text.concat(token.get(1));
                        for (int i = 2; i < token.size(); i++) {
                            text = text.concat(" " + token.get(i));
                        }
                        send(text);
                    }
                    else wrongCommand();
                    break;

                case "receive":
                    if (token.size() == 1) {
                        if (chatListener == null) {
                            System.out.println(Operation.NO_CHAT);
                        }
                        else chatListener.printMsgs();
                    }
                    else wrongCommand();
                    break;

                default:
                    wrongCommand();
            }
        }
    }

    /**
    * Metodo per connettere il proprio SocketChannel a quello
    * del server.
    */
    static void connectSocket() {
        try {
            socket = SocketChannel.open();
            InetAddress localAddress = InetAddress.getByName(serverName);
            socket.connect(new InetSocketAddress(localAddress, Config.portTCP));
        }
        catch (IOException exc) {
            System.out.println("\nImpossibile connettersi al server\n");
            System.exit(-1);
        }
    }

    /**
    * Metodo che registra l'user utilizzando la RMI sullo stub
    * @param usr username del registrando
    * @param psw la password del registrando
    */
    static void register(String usr, String psw) {
        RemoteTableInterface stub;
        Remote remoteObject;
        try {
            Registry registry = LocateRegistry.getRegistry(serverName, Config.portRegistryRMI);
            remoteObject = registry.lookup("REGISTER-TURING");
            stub = (RemoteTableInterface) remoteObject;
            Operation op = stub.register(usr, psw);
            System.out.println(op);
        }
        catch (NotBoundException | RemoteException e) {
            System.out.println(Operation.FAIL);
        }
    }

    /**
    * Metodo che effettua il login di un utente già registrato
    * @param usr username
    * @param psw password
    */
    static void login(String usr, String psw) {
        ByteBuffer a1 = ByteBuffer.wrap(usr.getBytes());
        ByteBuffer a2 = ByteBuffer.wrap(psw.getBytes());
        ByteBuffer a3 = ByteBuffer.allocate(4);

        notifier = new Notifier();
        notifierThread = new Thread(notifier);
        notifierThread.start();
        a3.putInt(notifier.getPort());
        a3.flip();

        Message request = new Message(Operation.LOGIN, a1, a2, a3);
        try {
            request.write(socket);
            Message reply = Message.read(socket);
            if (reply.getOp() == Operation.OK) loggedUser = usr;
            System.out.println(reply.getOp());
        }
        catch (IOException e) {
            System.out.println(Operation.FAIL);
        }
    }

    /**
     * Metodo per effettuare il logout.
     */
    static void logout() {
        Message request = new Message(Operation.LOGOUT);
        Message reply = null;
        try {
            request.write(socket);
            reply = Message.read(socket);
        }
        catch (IOException e) {
            System.out.println(Operation.FAIL);
            return;
        }

        if (reply.getOp() == Operation.OK) {
            loggedUser = null;
            chatAddress = null;
            notifierThread.interrupt();
            connectSocket();
        }
        System.out.println(reply.getOp());
    }

    /**
    * metodo per creare un documento
    * @param docName nome del documento da creare
    * @param numSec numero delle sezioni del documento creato
    */
    static void create(String docName, int numSec) {
        ByteBuffer a1 = ByteBuffer.wrap(docName.getBytes());
        ByteBuffer a2 = ByteBuffer.allocate(4);
        a2.putInt(numSec);
        a2.flip();

        Message request = new Message(Operation.CREATE, a1, a2);
        try {
            request.write(socket);
            Message reply = Message.read(socket);
            System.out.println(reply.getOp());
        }
        catch (IOException e) {
            System.out.println(Operation.FAIL);
        }
    }

    /**
    * Metodo per invitare un utente ad editare il documento
    * @param docName nome del documento che si invita ad editare
    * @param guest nome dell'utente invitato
    */
    static void share(String docName, String guest) {
        ByteBuffer a1 = ByteBuffer.wrap(docName.getBytes());
        ByteBuffer a2 = ByteBuffer.wrap(guest.getBytes());

        Message request = new Message(Operation.INVITE, a1, a2);
        try {
            request.write(socket);
            Message reply = Message.read(socket);
            System.out.println(reply.getOp());
        }
        catch (IOException e) {
            System.out.println(Operation.FAIL);
        }
    }

    /**
    * Metodo per scaricare una sezione e stampare il suo stato
    * @param docName nome del documento da cui estrarre la sezione
    * @param numSec numero della sezione
    */
    static void showSection(String docName, int numSec) {
        ByteBuffer a1 = ByteBuffer.wrap(docName.getBytes());
        ByteBuffer a2 = ByteBuffer.allocate(4);
        a2.putInt(numSec);
        a2.flip();

        Message request = new Message(Operation.SHOW_SECTION, a1, a2);
        Message reply = null;
        try {
            request.write(socket);
            reply = Message.read(socket);
        } catch (IOException e) {
            System.out.println(Operation.FAIL);
            return;
        }
        if (reply.getOp() != Operation.OK) {
            System.out.println(reply.getOp());
            return;
        }

        Vector<byte[]> chunk = reply.segment();
        int isBusy = ByteBuffer.wrap(chunk.get(0)).getInt();
        byte[] data = chunk.get(1);
        System.out.printf("\nLa sezione " + numSec + " di " + docName + " è ");
        if (isBusy != 0) {
            System.out.println("occupata");
        }
        else {
            System.out.println("libera");
            try { saveSection(docName, numSec, data);}
            catch (IOException e) {
                System.out.println("Errore nel download della sezione " +
                                    numSec + " di " + docName);
            }
        }
    }

    /**
    * Metodo per scaricare un documento e stampare lo stato delle sue sezioni
    * @param docName nome del documento
    */
    static void showDocument(String docName) {
        ByteBuffer a1 = ByteBuffer.wrap(docName.getBytes());
        Message request = new Message(Operation.SHOW_DOCUMENT, a1);
        Message reply = null;
        try {
            request.write(socket);
            reply = Message.read(socket);
        }
        catch (IOException e) {
            System.out.println(Operation.FAIL);
            return;
        }
        if (reply.getOp() != Operation.OK) {
            System.out.println(reply.getOp());
            return;
        }

        Vector<byte[]> chunk = reply.segment();
        ByteBuffer busySections = ByteBuffer.wrap(chunk.get(0));
        System.out.println("\nStato del documento:\n");
        for (int i = 1; i < chunk.size(); i++) {
            System.out.printf("La sezione " + i + " di " + docName + " è ");
            if (busySections.getInt() > 0) {
                System.out.println("occupata");
            }
            else {
                System.out.println("libera");
                try { saveSection(docName, i, chunk.get(i));}
                catch (IOException e) {
                    System.out.println("Errore nel download della sezione " + i + " di " + docName);
                }
            }
        }
    }

    /**
    * Metodo per stampare la lista dei document che si ha diritto ad editare
    */
    static void list() {
        Message request = new Message(Operation.LIST);
        Message reply = null;
        try {
            request.write(socket);
            reply = Message.read(socket);
        }
        catch (IOException e) {
            System.out.println(Operation.FAIL);
            return;
        }
        if (reply.getOp() != Operation.OK) {
            System.out.println(reply.getOp());
            return;
        }

        Vector<byte[]> chunk = reply.segment();
        System.out.println("\nLista dei documenti che puoi editare:\n");
        for (byte[] byteName : chunk) {
            String docName = new String(byteName);
            System.out.println(docName);
        }
    }

    /**
    * Metodo per scaricare un documento ed iniziare l'editing di una sua sezine, acquisendone l'esclusiva.
    * @param docName nome del documento
    * @param numSec numero della sezione di cui acquisire l'editing
    */
    static void edit(String docName, int numSec) {
        ByteBuffer a1 = ByteBuffer.wrap(docName.getBytes());
        ByteBuffer a2 = ByteBuffer.allocate(4);
        a2.putInt(numSec);
        a2.flip();

        Message request = new Message(Operation.START_EDIT, a1, a2);
        Message reply = null;
        try {
            request.write(socket);
            reply = Message.read(socket);
        }
        catch (IOException e) {
            System.out.println(Operation.FAIL);
            return;
        }
        if (reply.getOp() != Operation.OK) {
            System.out.println(reply.getOp());
            return;
        }

        Vector<byte[]> chunk = reply.segment();
        try { saveSection(docName, numSec, chunk.get(0));}
        catch (IOException e) {
            System.out.println("Errore nel download della sezione " + numSec + " di " + docName);
        }

        String strAddr = new String(chunk.get(1));
        try { chatAddress = InetAddress.getByName(strAddr);}
        catch (java.net.UnknownHostException e) {
            System.out.println(Operation.FAIL);
            return;
        }

        chatListener = new ChatListener(chatAddress);
        listener = new Thread(chatListener);
        listener.start();

        System.out.println(reply.getOp());
    }

    /**
    * Metodo per terminare l'editing della sezione di un documento, effettuandone l'upload
    * @param docName nome del documento
    * @param numSec numero della sezione
    */
    static void endEdit(String docName, int numSec) {
        ByteBuffer a1 = ByteBuffer.wrap(docName.getBytes());
        ByteBuffer a2 = ByteBuffer.allocate(4);
        ByteBuffer a3 = null;
        a2.putInt(numSec);
        a2.flip();

        Path path = Paths.get(loggedUser, docName, "section_" + numSec);
        try {
            FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
            int size = (int) channel.size();
            a3 = ByteBuffer.allocate(size);
            while (size > 0) {
                int tmp = channel.read(a3);
                if (tmp < 0) {
                    System.out.println(Operation.FAIL);
                    return;
                }
                size -= tmp;
            }
        }
        catch (IOException e) {
            System.out.println(Operation.FAIL);
            return;
        }
        a3.flip();


        Message request = new Message(Operation.END_EDIT, a1, a2, a3);
        Message reply = null;
        try {
            request.write(socket);
            reply = Message.read(socket);
        } catch (IOException e) {
            System.out.println(Operation.FAIL);
            return;
        }
        chatAddress = null;
        chatListener = null;
        if (listener != null) {
            listener.interrupt();
            listener = null;
        }
        System.out.println(reply.getOp());
    }

    /**
    * Metodo per inviare un messaggio sulla chat del documento che si sta editando
    * @param text il testo da inviare (verrà automaticamente corredato col mittente)
    */
    static void send(String text) {
        if (chatAddress == null) {
            System.out.println(Operation.NO_CHAT);
            return;
        }
        byte[] byteText = text.getBytes();
        if (byteText.length > 8192) {
            System.out.println(Operation.MESSAGE_TOO_LONG);
            return;
        }

        DatagramPacket dp = new DatagramPacket( byteText, byteText.length,
                                                chatAddress, Config.portChat);

        try {
            MulticastSocket ms = new MulticastSocket();
            ms.setTimeToLive(1);
            ms.send(dp);
            ms.close();
        }
        catch (IOException e) {
            System.out.println(Operation.FAIL);
            return;
        }
        System.out.println(Operation.OK);
    }

    /**
    * Metodo utilizzato dai precedenti per scrivere una sezione nel file system
    * @param docName nome del documento
    * @param num numero della sezione
    * @param data array di byte da scrivere
    * @throws IOException se occorrono errori nella scrittura del FileChannel
    */
    static void saveSection(String docName, int num, byte[] data) throws IOException {
        Path path = Paths.get(loggedUser, docName);
        Files.createDirectories(path);
        path = path.resolve("section_" + num);

        Files.deleteIfExists(path);
        FileChannel channel = FileChannel.open( path, StandardOpenOption.CREATE_NEW,
                                                        StandardOpenOption.WRITE);
        ByteBuffer section = ByteBuffer.wrap(data);
        while (section.hasRemaining()) {
            channel.write(section);
        }

    }


    /**
    * Metodo che stampa un messaggio di errore, indicando lanciare l'help
    */
    static void wrongCommand() {
        System.out.printf(
        "\nComando sbagliato! Digita \"help\" per la lista dei comandi disponibili\n"
        );
    }

    /**
    * Metodo che stampa l'help
    */
    static void printHelpMessage() {
        System.out.printf(
        "\nusage: COMMAND [ARGS...]\n\n" +
        "COMMANDS:\n\n    " +
        "register <username> <password>  registra l’utente\n    " +
        "login <username> <password>     effettua il login\n    " +
        "logout                          effettua il logout\n    " +
        "create <doc> <numsezioni>       crea un documento\n    " +
        "share <doc> <username>          condivide il documento\n    " +
        "show <doc> <sec>                mostra una sezione del documento\n    " +
        "show <doc>                      mostra l’intero documento\n    " +
        "list                            mostra la lista dei documenti\n    " +
        "edit <doc> <sec>                modifica una sezione del documento\n    " +
        "end-edit <doc> <sec>            fine modifica della sezione del doc\n    " +
        "send <msg>                      invia un msg in chat\n    " +
        "receive                         visualizza i msg ricevuti sulla chat\n\n"
        );
    }
}

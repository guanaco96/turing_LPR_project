import remote.RemoteTableInterface;
import server.Config;
import server.Message;
import server.Operation;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.net.*;
import java.util.*;

public class Client {

    static String loggedName;

    static void wrongCommand() {
        System.out.printf(
        "\nWrong command! Type \"help\" to list all available commands\n\n"
        );
    }

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
            "endedit <doc> <sec>             fine modifica della sezione del doc\n    " +
            "send <msg>                      invia un msg in chat\n    " +
            "receive                         visualizza i msg ricevuti sulla chat\n\n"
        );
    }

    public static void main(String[] args) {
        try {
            SocketChannel socket = SocketChannel.open();
            InetAddress localAddress = InetAddress.getByName(null);
            socket.connect(new InetSocketAddress(localAddress, Config.portTCP));

            // TODO
            int simu = (args.length > 0) ? 0 : 100;

            while (!Thread.interrupted()) {

                Vector<String> token = new Vector<>();
                // TODO
                if (simu < 4) {
                    if (args[0].equals("vale")) {
                        if (simu == 3) {
                            System.in.read();
                            System.out.printf("[share]: ");
                            token = new Vector<>();
                            token.add("share");
                            token.add("contra");
                            token.add("jaco");
                            simu++;
                        }
                        if (simu == 2) {
                            System.out.printf("[register]: ");
                            token = new Vector<>();
                            token.add("create");
                            token.add("contra");
                            token.add("5");
                            simu++;
                        }
                        if (simu == 1) {
                            System.out.printf("[login]: ");
                            token = new Vector<>();
                            token.add("login");
                            token.add("vale");
                            token.add("daje");
                            simu++;
                        }
                        if (simu == 0) {
                            System.out.printf("[create]: ");
                            token = new Vector<>();
                            token.add("register");
                            token.add("vale");
                            token.add("daje");
                            simu++;
                        }
                    }
                    if (args[0].equals("jaco")) {
                        if (simu == 3) {
                            System.in.read();
                            System.out.printf("[show]: ");
                            token = new Vector<>();
                            token.add("show");
                            token.add("contra");
                            token.add("1");
                            simu++;
                        }
                        if (simu == 2) {
                            System.out.printf("[register]: ");
                            token = new Vector<>();
                            token.add("create");
                            token.add("nietzsche");
                            token.add("7");
                            simu++;
                        }
                        if (simu == 1) {
                            System.out.printf("[login]: ");
                            token = new Vector<>();
                            token.add("login");
                            token.add("jaco");
                            token.add("mela");
                            simu++;
                        }
                        if (simu == 0) {
                            System.out.printf("[create]: ");
                            token = new Vector<>();
                            token.add("register");
                            token.add("jaco");
                            token.add("mela");
                            simu++;
                        }
                    }
                } else{

                    System.out.printf("turing >> ");
                    Scanner scanner = new Scanner(System.in);
                    String line = scanner.nextLine();
                    StringTokenizer tokenizer = new StringTokenizer(line);
                    while (tokenizer.hasMoreTokens()) {
                        token.add(tokenizer.nextToken());
                    }
                }

                switch (token.get(0)) {
                    case "help":
                        printHelpMessage();
                        break;

                    case "register":
                        if (token.size() != 3) {
                            wrongCommand();
                            break;
                        }
                        RemoteTableInterface stub;
                        Remote remoteObject;
                        Registry registry = LocateRegistry.getRegistry(Config.portRegistryRMI);
                        remoteObject = registry.lookup("REGISTER-TURING");
                        stub = (RemoteTableInterface) remoteObject;
                        Operation regOp = stub.register(token.get(1), token.get(2));
                        System.out.println(regOp);
                        break;

                    case "login":
                        if (token.size() != 3) {
                            wrongCommand();
                            break;
                        }
                        String usr = token.get(1);
                        String psw = token.get(2);
                        ByteBuffer a1 = ByteBuffer.wrap(usr.getBytes());
                        ByteBuffer a2 = ByteBuffer.wrap(psw.getBytes());
                        ByteBuffer a3 = ByteBuffer.allocate(4);
                        // TODO serve davvero mandargli questo dato che si trova nel file di configurazione???
                        a3.putInt(Config.portUDP);
                        a3.flip();

                        Message request = new Message(Operation.LOGIN, a1, a2, a3);
                        request.write(socket);
                        Message reply = Message.read(socket);
                        System.out.println(reply.getOp());
                        loggedName = usr;
                        break;

                    case "logout":
                        if (token.size() != 1) {
                            wrongCommand();
                            break;
                        }
                        request = new Message(Operation.LOGOUT);
                        reply = Message.read(socket);
                        System.out.println(reply.getOp());
                        loggedName = null;
                        break;

                    case "create":
                        if (token.size() != 3) {
                            wrongCommand();
                            break;
                        }
                        String documentName = token.get(1);
                        int n = Integer.parseInt(token.get(2));
                        a1 = ByteBuffer.wrap(documentName.getBytes());
                        a2 = ByteBuffer.allocate(4);
                        a2.putInt(n);
                        a2.flip();

                        request = new Message(Operation.CREATE, a1, a2);
                        request.write(socket);
                        reply = Message.read(socket);
                        System.out.println(reply.getOp());
                        break;

                    case "share":
                        if (token.size() != 3) {
                            wrongCommand();
                            break;
                        }
                        documentName = token.get(1);
                        String guest = token.get(2);
                        a1 = ByteBuffer.wrap(documentName.getBytes());
                        a2 = ByteBuffer.wrap(guest.getBytes());

                        request = new Message(Operation.INVITE, a1, a2);
                        request.write(socket);
                        reply = Message.read(socket);
                        System.out.println(reply.getOp());
                        break;

                    case "show":
                    if (token.size() != 2 && token.size() != 3) {
                        wrongCommand();
                        break;
                    }
                    if (token.size() == 3) {
                            documentName = token.get(1);
                            n = Integer.parseInt(token.get(2));
                            a1 = ByteBuffer.wrap(documentName.getBytes());
                            a2 = ByteBuffer.allocate(4);
                            a2.putInt(n);
                            a2.flip();

                            request = new Message(Operation.SHOW_SECTION);
                            request.write(socket);
                            reply = Message.read(socket);
                            Vector<byte[]> chunks = reply.segment();
                            int isBusy = ByteBuffer.wrap(chunks.get(0)).getInt();
                            ByteBuffer section = ByteBuffer.wrap(chunks.get(1));
                            if (reply.getOp() != Operation.OK) {
                                System.out.println(reply.getOp());
                                break;
                            }
                            try {
                                Path path = Paths.get(loggedName, documentName, "section_" + n);
                                FileChannel channel = FileChannel.open(path, StandardOpenOption.TRUNCATE_EXISTING);
                                while (section.hasRemaining()) {
                                    channel.write(section);
                                }
                            }
                            catch (IOException e) {
                                System.out.println(Operation.FAIL);
                                break;
                            }
                            System.out.println(reply.getOp());
                        }
                        if (token.size() == 2) {


                        }
                        break;

                    default:
                        wrongCommand();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }


    }

}

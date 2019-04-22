import remote.RemoteTableInterface;
import server.Config;
import server.Message;
import server.Operation;

import java.nio.*;
import java.nio.channels.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.net.*;
import java.util.*;

public class Client {
    public static void main(String[] args) {
        try {
            SocketChannel socket = SocketChannel.open();
            InetAddress localAddress = InetAddress.getByName(null);
            socket.connect(new InetSocketAddress(localAddress, Config.portTCP));

            while (!Thread.interrupted()) {
                System.out.printf("turing >> ");
                Scanner scanner = new Scanner(System.in);
                String line = scanner.nextLine();
                StringTokenizer tokenizer = new StringTokenizer(line);
                Vector<String> token = new Vector<>();
                while (tokenizer.hasMoreTokens()) {
                    token.add(tokenizer.nextToken());
                }
                switch (token.get(0)) {
                    case "register":
                        RemoteTableInterface stub;
                        Remote remoteObject;
                        Registry registry = LocateRegistry.getRegistry(Config.portRegistryRMI);
                        remoteObject = registry.lookup("REGISTER-TURING");
                        stub = (RemoteTableInterface) remoteObject;
                        stub.register(token.get(1), token.get(2));

                    case "login":
                        String usr = token.get(1);
                        String psw = token.get(2);
                        ByteBuffer a1 = ByteBuffer.wrap(usr.getBytes());
                        ByteBuffer a2 = ByteBuffer.wrap(psw.getBytes());
                        ByteBuffer a3 = ByteBuffer.allocate(4);
                        a3.putInt(Config.portUDP);
                        a3.flip();

                        Message request = new Message(Operation.LOGIN, a1, a2, a3);
                        request.write(socket);
                        Message reply = Message.read(socket);
                        System.out.println(reply.getOp());


                    default:
                        System.out.println("MESSAGGIO DI HELP");
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }


    }

}

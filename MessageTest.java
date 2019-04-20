import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.util.*;

import server.Operation;;
import server.Message;

class Server implements Runnable {
    int port;

    Server(int prt) {
        port = prt;
    }

    public void run() {
        try {
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.bind(new InetSocketAddress(InetAddress.getByName(null), port));

            while (!Thread.interrupted()) {
                SocketChannel sc = ssc.accept();
                System.out.println("Socket accettato lato server\n");

                Message msg = Message.read(sc);

                Vector<byte[]> chunks = msg.segment();
                System.out.println("\n---------------------------------\nServer side, dopo aver letto:");
                System.out.println(msg.getOp());
                System.out.println(new String(chunks.get(0)));
                System.out.println(new String(chunks.get(1)));


            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        }
    }
    class Client implements Runnable {
        int port;

        Client(int prt) {
            port = prt;
        }

        public void run() {
            try {
                SocketChannel sc = SocketChannel.open();
                sc.connect(new InetSocketAddress(InetAddress.getByName(null), port));

                String s1 = "Primo chunk";
                ByteBuffer b1 = ByteBuffer.wrap(s1.getBytes());
                String s2 = "Secondo chunk";
                ByteBuffer b2 = ByteBuffer.wrap(s2.getBytes());

                Message msg = new Message(Operation.OK, b1, b2);
                msg.write(sc);

                try { Thread.sleep(100); } catch (Exception e) {}
                Vector<byte[]> chunks = msg.segment();
                System.out.println("\n---------------------------------\nClient side, dopo aver scritto:");
                System.out.println(msg.getOp());
                System.out.println(new String(chunks.get(0)));
                System.out.println(new String(chunks.get(1)));

            }
            catch (Exception e) {}
        }
    }

    public class MessageTest {
        public static void main(String[] args) {
            Random rnd = new Random();
            int port = 1111;

            Thread st = new Thread(new Server(port));
            st.start();

            try {
                Thread.sleep(100);
            } catch (Exception e) {}

                Thread ct = new Thread(new Client(port));
                ct.start();

                try {
                    st.join();
                    ct.join();
                } catch (InterruptedException e) {
                    System.out.println("Server Interrotto");
                }
        }
    }

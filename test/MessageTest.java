package test;

import java.nio.*;
import java.nio.channels.*;
import java.net.*;

import server.Message;

class Server implements Runnable {
    int port;

    Server(int prt) {
        port = prt;
    }

    public void run() {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(InetAddress.getByName(null), port));

        while (!Thread.interrupted()) {
            SocketChannel sc = ssc.accept();
            Message msg = Message.read(sc);
            System.out.println(msg.getOp());
        }
    }
}

class Client implements Runnable {
    int port;

    Client(int prt) {
        port = prt;
    }

    void run() {
        SocketChannel sc = SocketChannel.open();
        sc.bind(new InetSocketAddress(InetAddress.getByName(null), port));

        Message msg = new Message(Operation.OK);
        msg.write(sc);
    }
}

public class MessageTest {
    public static void main(String[] args) {
        Random rnd = Random();
        int port = rnd.nextInt(1 << 16);

        while (true) {
            try {
                Socket sck = Socket(InetAddress.getByName(null), port);
                sck.close();
                break;
            }
            catch (IOException e) {
                port = (port + 1) % (1 << 16);
            }
        }

        Thread st = new Thread(new Server(port));
        st.start();

        Thread.sleep(100);

        Thread ct = new Thread(new Client(port));
        ct.start();
    }
}

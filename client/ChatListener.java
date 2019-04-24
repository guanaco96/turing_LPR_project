package client;

import server.Config;

import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;

public class ChatListener implements Runnable {

    private final int dpSize = 8192;
    private InetAddress ia;
    private Vector<String> pendingMsgs;

    /**
     *
     *
     */
    public ChatListener(InetAddress inetAddress) {
        ia = inetAddress;
        pendingMsgs = new Vector<>();
    }

    /**
     *
     *
     */
    synchronized public void printMsgs() {
        System.out.println("\nMessaggi ricevuti:\n");
        for (String s : pendingMsgs) {
            System.out.println(s);
        }
        pendingMsgs.removeAllElements();
    }

    /**
     *
     *
     */
    public void run() {
        MulticastSocket ms = null;
        try {
            ms = new MulticastSocket(Config.portChat);
            ms.joinGroup(ia);

            byte[] buffer = new byte[dpSize];
            DatagramPacket dp = new DatagramPacket(buffer, dpSize);

            ms.receive(dp);
            while (!Thread.interrupted()) {
                String msg = new String(dp.getData(), 0, dp.getLength());
                synchronized (this) {
                    pendingMsgs.add(msg);
                }
                ms.receive(dp);
            }
        }
        catch (IOException e) {
            System.out.println("\nErrore nella chat multicast\n");
        }
    }
}

package client;

import server.Config;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;

public class Notifier implements Runnable {
    private DatagramChannel dgc;

    /**
     *
     *
     */
    public Notifier() {
        try {
            // costruisco il datagramChannel e lo lego ad una porta effimera.
            dgc = DatagramChannel.open();
            dgc.bind(null);
        }
        catch(Exception e) {}
    }

    /**
     *
     *
     */
    public int getPort() {
        return dgc.socket().getLocalPort();
    }

    /**
     *
     *
     */
    public void run() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(100);
            dgc.receive(buffer);

            while (!Thread.interrupted()) {
                String notif = new String(buffer.array());
                System.out.printf("\n\n" + notif + "\n\n[turing] >> ");
                buffer = ByteBuffer.allocate(100);
                dgc.receive(buffer);
            }
        }
        catch (Exception e) {}
    }
}

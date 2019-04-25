package client;

import common.Config;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;

/**
 * Classe che implementa il main del thread del client che rimarrà in ascolto
 * di notifiche UDP da parte del server.
 *
 * @author Lorenzo Beretta, Matricola: 536242
 */
 public class Notifier implements Runnable {
    private DatagramChannel dgc;

    /**
     * Costruttore vuoto che apre il canale UDP
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
     * getter per il numero di porta associato al DatagramChannel,
     * indispensabile dato che questo viene assegnato automaticamente.
     * @return numero di porta
     */
    public int getPort() {
        return dgc.socket().getLocalPort();
    }

    /**
     * Main del thread: esegue un loop in cui legge la notifica e la stampa
     * finche non viene interrotto dal main del client.
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

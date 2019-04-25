package client;

import common.Config;

import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;

/**
 * Classe che implementa il main del thread del client che ascolta
 * i messaggi scritti in multicast tra gli utenti che editano lo stesso docuemnto.
 *
 * @author Lorenzo Beretta, Matricola: 536242
 */
public class ChatListener implements Runnable {

    private final int dpSize = 8192;
    private InetAddress ia;
    private Vector<String> pendingMsgs;

    /**
     * Costruttore
     * @param inetAddress indirizzo IP multicast del gruppo chat
     */
    public ChatListener(InetAddress inetAddress) {
        ia = inetAddress;
        pendingMsgs = new Vector<>();
    }

    /**
     * Metodo che stampa tutti i messaggi fino ad ora salvati
     * in una coda la svota.
     */
    synchronized public void printMsgs() {
        System.out.println("\nMessaggi ricevuti:\n");
        for (String s : pendingMsgs) {
            System.out.println(s);
        }
        pendingMsgs.removeAllElements();
    }

    /**
     * Main del thread che rimane in ascolto: dopo aver inizializzato
     * il MulticastSocket entra in un ciclo di lettura e salvataggio
     * nella coda dei messaggi.
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

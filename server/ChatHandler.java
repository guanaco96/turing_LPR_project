package server;

import java.util.*;
import java.io.*;

/**
 * -----------------DESCRIZIONE---------------
 *
 * @author Lorenzo Beretta, Matricola: 536242
 */
public class ChatHandler {

    private HashSet<String> addressSet;
    private int port;
    private Random random;
    private int numberOfAddresses = 1000000;

    /**
     *
     *
     */
    public ChatHandler(int prt) {
        addressSet = new HashSet<>();
        random = new Random();
    }

    /**
     *
     *
     */
     public synchronized String generateAddress() throws IOException {
        if (addressSet.size() == numberOfAddresses) throw new IOException();
        while (true) {
            int n = random.nextInt(numberOfAddresses) + 1;
            int q1 = 239;
            int q2 = (n >> 16) & 255;
            int q3 = (n >> 8) & 255;
            int q4 = n & 255;
            String address = q1 + "." + q2 + "." + q3 + "." + q4;

            if (!addressSet.contains(address)) {
                addressSet.add(address);
                return address;
            }
        }
    }

    /**
     *
     *
     */
    public synchronized void freeAddress(String address) {
        addressSet.remove(address);
    }

    /**
     *
     *
     */
    public int getPort() {
        return port;
    }
}

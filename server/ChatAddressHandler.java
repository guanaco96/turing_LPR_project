package server;

import java.util.*;
import java.io.*;

/**
 * -----------------DESCRIZIONE---------------
 *
 * @author Lorenzo Beretta, Matricola: 536242
 */
public class ChatAddressHandler {

    private HashSet<String> addressSet;
    private int port;
    private int base;
    private int bound;
    private Random random;

    /**
     *
     *
     */
    public ChatAddressHandler(int bs, int bnd, int prt) {
        addressSet = new HashSet<>();
        base = bs;
        bound = bnd;
        port = prt;
        random = new Random();
    }

    /**
     *
     *
     */
     public synchronized String generateAddress() throws Exception{
        if (addressSet.size() == bound) throw new Exception();
        while (true) {
            int n = base + random.nextInt(bound);
            int q1 = n >> 24;
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

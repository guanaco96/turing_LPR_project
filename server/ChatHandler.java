package server;

import java.util.*;
import java.io.*;

/**
 * Classe per la gestione degli indirizzi di multicast utilizzati
 * per le chat di lavoro di ogni documento
 *
 * @author Lorenzo Beretta, Matricola: 536242
 */
public class ChatHandler {

    private HashSet<String> addressSet;
    private Random random;
    private int numberOfAddresses = 1000000;

    /**
     * Costruttore vuoto
     */
    public ChatHandler() {
        addressSet = new HashSet<>();
        random = new Random();
    }

    /**
     * Genera un indirizzo casuale nel range [239.0.0.1, 239.0.0.1 + 1000000)
     * @return una String contenente l'indirizzo in dotted quad
     * @throws IOException nel caso in cui non ci siano indirizzi liberi
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
     * Metodo per liberare un indirizzo
     * @param address l'indirizzo sotto dorma di string da eleiminare dal set sottostante
     */
    public synchronized void freeAddress(String address) {
        addressSet.remove(address);
    }
}

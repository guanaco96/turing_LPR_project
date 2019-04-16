package Server;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * Questa classe definisce l'oggetto che contiene il messaggio
 * che client e server si scambiano. Esso è costituito da un header
 * lungo 8 bytes, costituito dal tipo di operazione e la lunghezza
 * totale del messaggio, e un buffer contenente il corpo del messaggio.
 * Qusta classe implementa i metodi per scrivere e leggere messaggi su
 * SocketChannel.
 *
 * @author Lorenzo Beretta, Matricola: 536242
 */
public class Message {

    private Operation op ;
    private int size;
    private ByteBuffer buffer;

    /**
     * Costruttore pubblico ad argomenti variabili, codifica la seguenza di chunks
     * come giustapponendo le coppie chunk.size() @ chunk nel ByteBuffer. 
     * @param operation header del messaggio
     * @param chunks list di byte[] da inserire nel corpo
     */
    public Message(Operation operation, byte[]... chunks) {
        op = operation;
        size = 0;

        for (byte[] chunk : chunks) size += chunk.length + 4;
        buffer = ByteBuffer.allocate(size);

        for(byte[] chunk : chunks) {
            buffer.putInt(chunk.size());
            buffer.put(chunk);
        }

        buffer.flip();
    }

    /**
     * Sovrascrive this con il messaggio letto dal channel
     * @param channel il SocketChannel dal quale leggere il messaggio
     * @throws IOException se readBytes la solleva
     */
    public void read(SocketChannel channel) throws IOException {
        ByteBuffer hdr = ByteBuffer.allocate(8);
        readBytes(channel, hdr, 8);
        hdr.flip();
        op = Operation.getOperation(hdr.getInt());
        size = hdr.getInt();
        buffer = ByteBuffer.allocate(size);
        readBytes(channel, buffer, size);
    }

    /**
     * Scrive this sul channel
     * @param channel il SocketChannel sul quale scrivere
     * @throws IOException se writeBytes la solleva
     */
    public void write(SocketChannel channel) throws IOException {
        ByteBuffer hdr = ByteBuffer.allocate(8);
        hdr.putInt(op.number);
        hdr.putInt(size);
        hdr.flip();
        writeBytes(channel, hdr, 8);
        writeBytes(channel, buffer, size);
    }

    /**
     * Legge i bytes da un socketchannel su un ByteBuffer
     * @param channel SocketChannel dal quale leggere i dati
     * @param buffer ByteBuffer sul quale scrivere i dati letti
     * @param size numero di byte da leggere
     * @throws IOException se la SocketChannel.read la solleva o se si raggiunge End of Stream
     */
    private void readBytes(SocketChannel channel, ByteBuffer buffer, int size) throws IOException {
        while (size > 0) {
            int tmp = channel.read(buffer);
            if (tmp < 0) throw new IOException();
            size -= tmp;
        }
    }

    /**
     * Scrive i bytes su un socketchannel da un ByteBuffer
     * @param channel SocketChannel sul quale scrivere i dati
     * @param buffer ByteBuffer dal quale leggere i dati scritti\
     * @param size numero di byte da scrivere
     * @throws IOException se la SocketChannel.write la solleva
     */
    private void writeBytes(SocketChannel channel, ByteBuffer buffer, int size) throws IOException {
        while (size > 0) {
             size -= channel.write(buffer);
        }
    }

}

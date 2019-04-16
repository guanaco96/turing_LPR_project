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
<<<<<<< HEAD
    private int length;
    private Vector<> chunkLength;
    private ByteBuffer buffer;

    public Message(Operation op, byte[]... chunks) {
        this.op = op;
        len = 0;

        for (byte[] chunk : chunks) {
            chunkLength.add(chunk.length);
            len += chunkLength.lastElement();
        }

        buffer = ByteBuffer.allocate(len);
        for(byte[] chunk : chunks) {
            buffer.put(chunk);
        }
=======
    private int size;
    private ByteBuffer buffer;

    /**
     * Costruttore ad argomenti variabili.
     * @param operation header del messaggio
     * @param chunks list di byte[] da inserire nel corpo
     */
    public Message(Operation operation, byte[]... chunks) {
        op = operation;
        size = 0;

        for (byte[] chunk : chunks) size += chunk.length + 4;
        buffer = ByteBuffer.allocate(size);
>>>>>>> 4a84dcb740a8542f700af768163ba9b02c6b4785

        for(byte[] chunk : chunks) buffer.put(chunk);
        buffer.flip();
    }

<<<<<<< HEAD
    public void read(SocketChannel channel) {
        Bytebuffer hdr = ByteBuffer.allocate(4);
        readBytes(channel, hdr, 4);
        this.op = Operation.get(hdr.getInt());
        while ()
=======
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
>>>>>>> 4a84dcb740a8542f700af768163ba9b02c6b4785
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
            int tmp = channel.write(buffer);
<<<<<<< HEAD
            if (tmp < 0) throw new IOException();
=======
>>>>>>> 4a84dcb740a8542f700af768163ba9b02c6b4785
            size -= tmp;
        }
    }

}

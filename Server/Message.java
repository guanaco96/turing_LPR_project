package Server;

import java.io.*;
import java.nio.*;

public class Message {

    private Operation op ;
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

        buffer.flip();
    }

    public void read(SocketChannel channel) {
        Bytebuffer hdr = ByteBuffer.allocate(4);
        readBytes(channel, hdr, 4);
        this.op = Operation.get(hdr.getInt());
        while ()
    }


    private void readBytes(SocketChannel channel, ByteBuffer buffer, int size) throws IOException {
        while (size) {
            int tmp = channel.read(buffer);
            if (tmp < 0) throw new IOException();
            size -= tmp;
        }
    }

    private void writeBytes(SocketChannel channel, ByteBuffer buffer, int size) throws IOException {
        while (size) {
            int tmp = channel.write(buffer);
            if (tmp < 0) throw new IOException();
            size -= tmp;
        }
    }

}

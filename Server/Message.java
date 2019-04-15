package Server;

import java.io.*;
import java.nio.*;

public class Message {

    private Operation op ;
    private Vector<int> chunkLength;
    private ByteBuffer buffer;

    public Message(Operation op, Byte[]... chunks) {
        this.op = op;
        int dim = 0;

        for (Byte[] chunk : chunks) {
            chunkLength.add(chunk.length);
            dim += chunkLength.lastElement();
        }

        buffer.allocate(dim);
        for(Byte[] chunk : chunks) {
            buffer.put(chunk);
        }

        buffer.flip();
    }

    public void read(SocketChannel channel) {
        
    }


    private void readBytes(SocketChannel channel, ByteBuffer buffer, int size) throws IOException {
        while (size) {
            int tmp = channel.read(buffer);
            if (tmp < 0) throw new IOException;
            size -= tmp;
        }
    }

    private void writeBytes(SocketChannel channel, ByteBuffer buffer, int size) throws IOException {
        while (size) {
            int tmp = channel.write(buffer);
            if (tmp < 0) throw new IOException;
            size -= tmp;
        }
    }

}

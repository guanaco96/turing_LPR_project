package server;

public class Config {

    static String basePath;
    static int maxDocumentSize = 1 << 16;
    static int threadsInPool = 8;
    static int timeOut;

    static int portTCP;
    static int portUDP;
    static int portRMI;
    static int portChat;

    static int addressBase;
    static int addressBound;
}

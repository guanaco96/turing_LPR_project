package server;

public class Config {

    public static String basePath = "files";
    public static int maxDocumentSize = 1 << 16;
    public static int threadsInPool = 10;
    // time (in milliseconds ) the selector wait for a
    // ready SelectableChannel to get ready before unblocking
    public static int selectorTime = 1000; //TODO should be 10000

    public static int portTCP = 40000;
    public static int portUDP = 40001;
    public static int portRegistryRMI = 40002;
    public static int portChat = 40003;
}

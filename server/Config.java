package server;

public class Config {

    public static String basePath = "~/lorenzo/Documenti/reti_di_calcolatori/turing_LPR_project";
    public static int maxDocumentSize = 1 << 16;
    public static int threadsInPool = 10;
    // time (in milliseconds ) the selector wait for a
    // ready SelectableChannel to get ready before unblocking
    public static int selectorTime = 1000; //TODO should be 10000

    public static int portTCP = 40000;
    public static int portUDP = 40001;
    public static int portRegistryRMI = 40002;
    public static int portChat = 40003;

    public static int addressBase = 250 << 24;
    public static int addressBound = 1 << 24;
}

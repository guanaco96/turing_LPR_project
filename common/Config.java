package common;

/**
 * File contenente alcuni parametri di serve e client impostabili
 *
 * @author Lorenzo Beretta, Matricola: 536242
 */
public class Config {

    public static String basePath = "files";
    public static int maxDocumentSize = 1 << 16;
    public static int threadsInPool = 10;
    public static int selectorTime = 1000;

    public static int portTCP = 40000;
    public static int portUDP = 40001;
    public static int portRegistryRMI = 40002;
    public static int portChat = 40003;
}

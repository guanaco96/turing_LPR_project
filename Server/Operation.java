package Server;

/**
 * Questa enum definisce i tipi di messaggio inviati tra Server e client.
 *
 * @author Lorenzo Beretta, Matricola: 536242
 */
public enum Operation {

    // richieste del Client
    LOGIN(0),
    INVITE(1),
    CREATE(2),
    EDIT(3),
    END_EDIT(4),
    SHOW_SECTION(5),
    SHOW_DOCUMENT(6),
    CHAT(7),

    // risposte dal Server
    OK(10),
    FAIL(11);

    int number;
    static private Operation[] array = new Operation[12];

    /**
     * Costruttore
     * @param n intero non negativo
     */
    Operation(int n) {
        number = n;
    }

    // inizializzazione statica della mappa inversa
    static {
        for (Operation op : Operation.values()) {
            array[op.number] = op;
        }
    }

    /**
    * Metodo statico che fornisce la mappa inversa
    * @param n intero non negativo
    * @return operazione corrispondente all'intero n
    */
    public static Operation getOperation(int n) {
        return array[n];
    }

    /**
    * Overriding del toString
    * @return una descrizione del tipo di operazione
    */
    public String toString() {
        switch (this) {
            case OK: return "Operazione OK";
            case FAIL: return "Operazione Fallita";

            default: return "Operazione Ignota";
        }
    }
}

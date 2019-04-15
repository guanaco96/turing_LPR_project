package Server;

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
    Operation(int number) {
        this.number = number;
    }
    
    // inizializzazione statica della mappa inversa
    static private Operation[] array = new Operation[12];
    static {
        for (Operation op : Operation.values()) {
            array[op.number] = op;
        }
    }

    // metodo statico che fornisce la mappa inversa
    public static Operation get(int x) {
        return array[x];
    }

    // ci interessa fare l'overriding del toString solo per le operaizoni di richiesta
    public String toString() {
        switch (this) {
            case OK: return "Operazione OK";
            case FAIL: return "Operazione Fallita";

            default: return "Operazione Ignota";
        }
    }
}

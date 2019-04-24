package server;

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
    START_EDIT(3),
    END_EDIT(4),
    SHOW_SECTION(5),
    SHOW_DOCUMENT(6),
    CHAT(7), // ?? SERVE DAVVERO QUESTO TIPO DI OPERAIZONE ??? potremmo fornire l'indirizzo in caso di start edit..
    LIST(8),
    LOGOUT(9),


    // risposte dal Server
    OK(10),
    FAIL(11),
    UNAUTHORIZED(12),
    SECTION_BUSY(13),
    SECTION_SIZE_EXCEEDED(14),
    WRONG_PSW(15),
    WRONG_REQUEST(16),
    ALREADY_LOGGED(17),
    NOT_LOGGED(18),
    USER_UNKNOWN(19),
    DOCUMENT_UNKNOWN(20),
    DUPLICATE_DOCUMENT(21),
    SECTION_UNKNOWN(22),
    NOTIFICATION(23),
    USERNAME_EXISTS(24),
    MESSAGE_TOO_LONG(25),
    NO_CHAT(26);

    int number;
    static private Operation[] array = new Operation[30];

    // inizializzazione statica della mappa inversa
    static {
        for (Operation op : Operation.values()) {
            array[op.number] = op;
        }
    }

    /**
     * Costruttore della enum
     * @param n intero non negativo
     */
    Operation(int n) {
        number = n;
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
        String s = "\t  ";
        switch (this) {
            case OK:                    return s + "Operazione eseguita con successo";
            case FAIL:                  return s + "Operazione fallita";
            case UNAUTHORIZED:          return s + "Operazione non autorizzata";
            case SECTION_BUSY:          return s + "La sezione è occupata";
            case SECTION_SIZE_EXCEEDED: return s + "La sezione eccede le dimensioni massime";
            case WRONG_PSW:             return s + "Password errata";
            case WRONG_REQUEST:         return s + "Richiesta mal formattata";
            case ALREADY_LOGGED:        return s + "Sei già loggato con un altro user";
            case NOT_LOGGED:            return s + "Non sei ancora loggato! Effettua il logIn e ritenta";
            case USER_UNKNOWN:          return s + "L'utente non è esiste";
            case DOCUMENT_UNKNOWN:      return s + "Il documento non esiste";
            case DUPLICATE_DOCUMENT:    return s + "Documento già presente";
            case NOTIFICATION:          return s + "Notifica";
            case USERNAME_EXISTS:       return s + "Username già occupato";
            case MESSAGE_TOO_LONG:      return s + "Messaggio troppo lungo";
            case NO_CHAT:               return s + "Non sei connesso ad alcuna chat";

            default:                    return s + "Operazione Ignota";
        }
    }
}

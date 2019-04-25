package server;

import server.User;
import common.RemoteTableInterface;
import common.Operation;

import java.util.concurrent.*;
import java.rmi.*;
import java.rmi.server.*;

/**
 * Imlementazione dell'interfaccia RemoteTableInterface, utile per implementare
 * la registrazione del client tramite RMI del metodo register
 *
 * @author Lorenzo Beretta, Matricola: 536242
 */
public class RemoteTableImplementation extends RemoteServer implements RemoteTableInterface {

    ConcurrentHashMap<String,User> userMap;

    /**
     * Costruttore
     * @param usrMp mappa userName -> User che contiene gli utenti registrati
     */
    public RemoteTableImplementation(ConcurrentHashMap<String,User> usrMp) {
        userMap = usrMp;
    }

    /**
     * Metodo da invocare con RMI per registrare un utente a TURING
     * @param user username dell'utente
     * @param password password associata a quell'account
     * @return l'esito dell'invocazione codificato con la enum operation
     * @throws RemoteException nel caso ci siano malfunzionamenti nell'RMI
     */
    public Operation register(String user, String password) throws RemoteException {
        if (userMap.putIfAbsent(user, new User(user, password)) == null) return Operation.OK;
        else return Operation.USERNAME_EXISTS;
    }
}

package server;

import remote.RemoteTableInterface;
import server.User;
import server.Operation;

import java.util.concurrent.*;
import java.rmi.*;
import java.rmi.server.*;

/**
 * -----------------DESCRIZIONE---------------
 *
 * @author Lorenzo Beretta, Matricola: 536242
 */
public class RemoteTableImplementation extends RemoteServer implements RemoteTableInterface {

    ConcurrentHashMap<String,User> userMap;

    /**
     *
     *
     */
    public RemoteTableImplementation(ConcurrentHashMap<String,User> usrMp) {
        userMap = usrMp;
    }

    /**
     *
     *
     */
    public Operation register(String user, String password) throws RemoteException {
        if (userMap.putIfAbsent(user, new User(user, password)) == null) return Operation.OK;
        else return Operation.USERNAME_EXISTS;
    }
}

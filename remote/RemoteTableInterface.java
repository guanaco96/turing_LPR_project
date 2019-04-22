package remote;

import server.Operation;

import java.rmi.*;

/**
 * -----------------DESCRIZIONE---------------
 *
 * @author Lorenzo Beretta, Matricola: 536242
 */
 public interface RemoteTableInterface extends Remote {

     public Operation register(String user, String password) throws RemoteException;
 }

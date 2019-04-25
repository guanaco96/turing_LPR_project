package common;

import common.Operation;

import java.rmi.*;

/**
 * Interfaccia comune a client e server per la gestione della RMI
 *
 * @author Lorenzo Beretta, Matricola: 536242
 */
 public interface RemoteTableInterface extends Remote {

     public Operation register(String user, String password) throws RemoteException;
 }

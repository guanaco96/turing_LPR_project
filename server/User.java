package server;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.net.*;

/**
 * -----------------DESCRIZIONE---------------
 *
 * @author Lorenzo Beretta, Matricola: 536242
 */
public class User {

    private String username;
    private String password;
    private HashSet<Document> myDocuments;
    private boolean isLogged;
    private Document currentlyEditing;
    private InetSocketAddress clientAddress;

    /**
     *
     *
     */
    User(String usr, String psw) {
        username = usr;
        password = psw;
        myDocuments = new HashSet<>();
        isLogged = false;
        currentlyEditing = null;
        clientAddress = null;
    }

    /**
     *
     *
     */
     synchronized Operation logIn(String psw, InetSocketAddress address) {
         if (isLogged) return Operation.LOGGED_YET;
         if (!password.equals(psw)) return Operation.WRONG_PSW;

         isLogged = true;
         clientAddress = address;
         return Operation.OK;
     }

     /**
      *
      *
      */
      synchronized Document logOut() {
          isLogged = false;
          Document ret = currentlyEditing;
          currentlyEditing = null;
          clientAddress = null;
          return ret;
      }

     /**
      *
      *
      */
      synchronized Operation startEdit(Document document) {
          if (currentlyEditing != null) return Operation.UNAUTHORIZED;
          currentlyEditing = document;
          return Operation.OK;
      }

      /**
       *
       *
       */
       synchronized Operation endEdit() {
           if (currentlyEditing == null) return Operation.UNAUTHORIZED;
           currentlyEditing = null;
           return Operation.OK;
       }

       /**
         *
         *
         */
         synchronized void sendNotification(String host, String document, DatagramChannel channel) {
             if (!isLogged) return;
             String msg = "[" + host + "] ti ha invitato a modificare " + document;
             ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
                try {
                    channel.send(buffer, clientAddress);
                }
                catch(IOException e) {
                    // non è grave se si perde una notifica, stesso motivo per cui usiamo UDP
                }
         }

         /**
          *
          *
          */

          /**
           *
           *
           */

}

import server.*;
import remote.*;

import java.nio.*;
import java.nio.channels.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.net.*;

public class Test_1 {
    public static void main(String[] args) {
        RemoteTableInterface stub;
        Remote remoteObject;

        //------------------- REGISTRAZIONE TRAMITE RMI ----------------------
        try {
            Registry registry = LocateRegistry.getRegistry(Config.portRegistryRMI);
            remoteObject = registry.lookup("REGISTER-TURING");
            stub = (RemoteTableInterface) remoteObject;
            stub.register("valeria_ghigo", "dajeforte");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //--------------------- MI CONNETTO AL SOCKET E MANDO MESSAGGI ---------
        try {
            SocketChannel socket = SocketChannel.open();
            InetAddress localAddress = InetAddress.getByName(null);
            socket.connect(new InetSocketAddress(localAddress, Config.portTCP));

            //--------------- EFFETTUO IL LOGIN ----------
            String usr = "valeria_ghigo";
            String psw = "dajeforte";
            ByteBuffer a1 = ByteBuffer.wrap(usr.getBytes());
            ByteBuffer a2 = ByteBuffer.wrap(psw.getBytes());
            ByteBuffer a3 = ByteBuffer.allocate(4);
            a3.putInt(Config.portUDP);
            a3.flip();

            Message request = new Message(Operation.LOGIN, a1, a2, a3);
            request.write(socket);
            Message reply = Message.read(socket);
            System.out.println(reply.getOp());

            //--------------- CREO UN DOCUMENTO -------------
            String documentName = "costratto_scassa_cazzo";
            a1 = ByteBuffer.wrap(documentName.getBytes());
            a2 = ByteBuffer.allocate(4);
            a2.putInt(8);
            a2.flip();

            request = new Message(Operation.CREATE, a1, a2);
            request.write(socket);
            reply = Message.read(socket);
            System.out.println(reply.getOp());

            //----------- INVITO UN ALTRO UTENTE --------

            socket.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}

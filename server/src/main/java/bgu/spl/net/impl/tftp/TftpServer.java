package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.Server;

import java.io.File;


public class TftpServer {
    public static void main(String[] args) {
     //     Server.threadPerClient(7777,() -> new TftpProtocol(),() -> new TftpEncoderDecoder()).serve();
        try{
            int port = Integer.parseInt(args[0]);
            Server.threadPerClient(
                    port, //port
                    () -> new TftpProtocol(), //protocol factory
                    () -> new TftpEncoderDecoder() //Tftp encoder decoder factory
            ).serve();

        }
        catch (NumberFormatException e)
        {
            System.out.println("Invalid port");
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }

    }
}

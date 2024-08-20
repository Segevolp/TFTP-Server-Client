package bgu.spl.net.impl.tftp;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TftpClient {
    Keyboard keyboard;
    Listener listener;
    public static void main(String[] args) throws IOException {
        try
        {
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            TftpClient client = new TftpClient();
            client.start(host,port);
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

    /**
     * client start function- build a connections and starts the threads
     * @param host which host to connect to
     * @param port port to connect to server
     */
    private void start(String host,int port) throws IOException {
        try (Socket sock = new Socket(host, port);
             BufferedInputStream in = new BufferedInputStream(sock.getInputStream());
             BufferedOutputStream out = new BufferedOutputStream(sock.getOutputStream())) {
            System.out.println("Connected to the server!");
            Bridge bridge = new Bridge(
                    new TftpProtocol(), out, new TftpEncoderDecoder(), in, sock,
                    this::terminate
            );
            this.keyboard = new Keyboard(bridge,Config.getInstance());
            this.listener = new Listener(bridge,keyboard);
            Thread t1 = new Thread(keyboard);
            t1.start();
            Thread t2 = new Thread(listener);
            t2.start();

            t1.join();
        }
        catch (IOException | InterruptedException e ){
            e.printStackTrace();
       }
    }
    public void terminate(){
       keyboard.terminate();
       listener.terminate();
    }

}

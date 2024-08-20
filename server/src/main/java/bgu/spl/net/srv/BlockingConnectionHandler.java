package bgu.spl.net.srv;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    private final BidiMessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;

    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, BidiMessagingProtocol<T> protocol) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { //just for automatic closing
            int read;

            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());

            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    System.out.println("in: " + bytesTo2Hex((byte[])nextMessage));
                    protocol.process(nextMessage);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    private static String bytesTo2Hex(byte[] bytes) {
        StringBuilder builder1 = new StringBuilder();
        builder1.append("[ ");
        for(int i =0;i<bytes.length;i++)
        {
            builder1.append(String.format("%02x" , bytes[i] ));
            builder1.append(", ");
        }
        builder1.delete(builder1.length()-2,builder1.length()-1);
        builder1.append("] ");
        return builder1.toString();

    }
    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    /**
     *
     * @param msg - message to send.
     * @return false iff an exception occurred (socket closed).
     */
    @Override
    public boolean send(T msg) {
        try
        {
            if(msg!=null)
            {
                System.out.println("out: " + bytesTo2Hex((byte[])msg)); //TODO : REMOVE.
                out.write(encdec.encode(msg));
                out.flush();
            }
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }

    }
}

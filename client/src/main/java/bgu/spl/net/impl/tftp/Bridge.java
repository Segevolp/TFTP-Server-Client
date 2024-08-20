package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * this class is to hold all the objects that both threads need
 */
public class Bridge {
    public final TftpProtocol prot;
    public final BufferedOutputStream out;
    public final TftpEncoderDecoder encdec;
    public final BufferedInputStream in;
    public final Socket sock;
    public final TerminateCallBack callBack;
    public Bridge(TftpProtocol prot,BufferedOutputStream out,TftpEncoderDecoder encdec, BufferedInputStream input, Socket serverSocket, TerminateCallBack callBack){
        this.prot = prot;
        this.out = out;
        this.encdec = encdec;
        this.in = input;
        this.sock = serverSocket;
        this.callBack = callBack;
    }


    /**
     * call back to terminate program
     */
    public void terminate(boolean bruteForce){
        if(bruteForce)
        {
            System.exit(0);
        }
        else
        {
            try {
                sock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            callBack.call();
        }
    }
}

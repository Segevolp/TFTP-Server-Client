package bgu.spl.net.impl.tftp;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class Listener implements Runnable{
    private boolean shouldTerminate;
    private Bridge bridge;
    private Keyboard keyboard;
    private boolean lastCommandSucceeded;
    private Thread listenerThread;

    public Listener(Bridge bridge, Keyboard keyboard){
        this.bridge = bridge;
        this.keyboard = keyboard;
    }

    /**
     * main loop for listener
     * waits for packets to be recieved from server and processes them
     * if needed sends back packets to server.
     */
    @Override
    public void run() {
        listenerThread = Thread.currentThread();
        shouldTerminate = false;
        int read;
        try{
            while (!shouldTerminate() && (read = bridge.in.read())>=0) {
                byte[] nextMessage = bridge.encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                 //   System.out.println("in: " + bytesTo2Hex(nextMessage));
                    byte[] msgToSend=bridge.prot.process(nextMessage);
                    if(msgToSend != null){
                       // System.out.println("out: " + bytesTo2Hex(msgToSend));
                        bridge.out.write(msgToSend);
                        bridge.out.flush();
                    }
                    if(bridge.prot.getFinishedCommand()){
                        synchronized (keyboard){
                            keyboard.notify();
                        }
                    }

                }
            }
        }
        catch (SocketException e){
            if(!shouldTerminate())
            {
                System.out.println("Lost connection to server. Exiting program!");
            }
            bridge.terminate(true);
        }
        catch (IOException e) {
            e.printStackTrace();
            bridge.terminate(false);
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
    public boolean shouldTerminate()
    {
        return shouldTerminate;
    }
    public void terminate()
    {
        shouldTerminate = true;
        listenerThread.interrupt();
    }
}

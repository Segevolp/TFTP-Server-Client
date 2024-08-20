package bgu.spl.net.impl.tftp;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;
public class Keyboard implements Runnable{
    private boolean shouldTerminate;
    private final Bridge bridge;
    private final Config config;
    public Keyboard(Bridge bridge,Config config){
        this.bridge = bridge;
        this.config = config;
    }

    /**
     * main loop for keyboard
     * waits for inputs, encodes them and sends bytes to the server
     */
    @Override
    public void run() {
        shouldTerminate = false;
        byte[] message;
        Scanner scanner = new Scanner(System.in);
        while(!shouldTerminate())
        {
            String[] command = scanner.nextLine().split(" ");
            message = bridge.prot.convertCommandToMessage(command);
            String name;
            String[] temp = new String[command.length-1];
            if(message != null)
            {
                try{
                    if(command[0].equals("RRQ")){
                        System.arraycopy(command, 1, temp, 0, temp.length);
                        name = String.join(" ",temp);
                        File file = new File(config.fileDirectory,  name);
                        if(file.exists()){
                            System.out.println("file already exists");
                        }
                        else{
                            file.createNewFile();
                            bridge.prot.setCurrentRRQFile(file);
                            bridge.out.write(bridge.encdec.encode(message));
                            bridge.out.flush();
                            synchronized (this)
                            {
                                try{
                                    wait();
                                } catch (InterruptedException ignored) {}

                            }
                            if(file.exists()){
                                System.out.println("RRQ " + name + " complete.");
                            }
                        }
                    } else if (command[0].equals("WRQ")) {
                        System.arraycopy(command, 1, temp, 0, temp.length);
                        name = String.join(" ",temp);
                        File file = new File(config.fileDirectory,  name);
                        if(!file.exists()){
                            System.out.println("file does not exists");
                        }
                        else{
                            bridge.prot.setCurrentWRQFile(file,new FileInputStream(file));
                            bridge.out.write(bridge.encdec.encode(message));
                            bridge.out.flush();
                            synchronized (this)
                            {
                                try{
                                    wait();
                                } catch (InterruptedException ignored) {}

                            }
                            if(bridge.prot.getCompletedCommand())
                                System.out.println("WRQ " + name + " complete.");

                        }
                    }
                    else{
                        bridge.out.write(bridge.encdec.encode(message));
                        bridge.out.flush();
                        synchronized (this)
                        {
                            try{
                                wait();
                            } catch (InterruptedException ignored) {}
                        }

                        if(command[0].equals("DISC")){
                            bridge.terminate(false);
                        }
                    }

                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else{
                System.out.println("Invalid command");
            }
        }
        scanner.close();
    }

    public boolean shouldTerminate()
    {
        return shouldTerminate;
    }

    public void terminate()
    {
        shouldTerminate = true;

    }
}

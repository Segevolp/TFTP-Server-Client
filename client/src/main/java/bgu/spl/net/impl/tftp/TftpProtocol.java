package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessagingProtocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class TftpProtocol implements MessagingProtocol<byte[]> {
    private Config config;
    private boolean finishedCommand = false;
    private File currentRRQFile;
    private File currentWRQFile;

    private FileInputStream fileInputStream;
    private byte[] sendDataBuffer;
    private short sendDataBlockNumber;
    private boolean completedCommand = true;


    /**
     * this function gets a message and processes it according to our protocol
     * @param message the received message
     * @return
     */
    @Override
    public byte[] process(byte[] message) {
        if(message.length<2)
        {
            System.out.println("impossible state in process");
            throw new IllegalArgumentException();
        }
        short opCode = ( short ) ((( short ) message [0]) << 8 | ( short ) ( message [1]) );
        PacketType pType = PacketType.fromOpCode(opCode);
        switch (pType)
        {
            case DATA:
                finishedCommand = false;
                if(currentRRQFile!=null){ // RRQ mode
                    byte[] data = getDataFromMsg(message);
                    saveData(data);
                    short blockNumber = ( short ) ((( short ) (message [4]& 0xFF) << 8 | ( short ) ( message [5]& 0xFF) ));
                    byte[] ackToSend = encodeACK(blockNumber);
                    return ackToSend;
                }
                else{ //DIRQ mode
                    byte[] dirs = getDataFromMsg(message);
                    if(dirs.length<512){
                        finishedCommand = true;
                    }
                    String byteString = new String(dirs, StandardCharsets.UTF_8);
                    String[] words = byteString.split("\u0000"); //byte 0 encode acc to utf8 is this
                    if(dirs[dirs.length-1]==0)
                    {
                        for(String word: words){
                            System.out.println(word);
                        }
                    }
                    else
                    {
                        for(int i =0;i<words.length-1;i++)
                        {
                            System.out.println(words[i]);
                        }
                        System.out.print(words[words.length-1]);
                    }
                    short blockNumber = ( short ) ((( short ) (message [4]& 0xFF)) << 8 | ( short ) ( message [5] & 0xFF) );
                    byte[] ackToSend = encodeACK(blockNumber);
                    return ackToSend;
                }
            case ACK:
                System.out.println("ACK " + Util.byteArrayToShort(new byte[]{message[2],message[3]}));
                if(currentWRQFile!=null){
                    finishedCommand = false;
                    completedCommand = false;
                    return sendNextData();
                }
                else
                {
                    completedCommand = true;
                    finishedCommand = true;
                }
                break;
            case ERROR:
                if(currentRRQFile != null && currentRRQFile.exists()){
                    currentRRQFile.delete();
                    currentRRQFile = null;
                }
                if(currentWRQFile != null && currentWRQFile.exists()){
                    currentWRQFile = null;
                }
                completedCommand = false;
                finishedCommand = true;
                System.out.println("Error " + Util.byteArrayToShort(new byte[]{message[2],message[3]}) + " " + decodeErrorMsg(message));
                break;
            case BCAST:
                String addOrDel;
                if(message[2]==0)
                {
                    addOrDel = "del";
                }
                else
                {
                    addOrDel = "add";
                }
                System.out.println("BCAST " + addOrDel + " " + decodeBCASTFileNime(message));
                break;
        }
        return null;
    }

    /**
     * this function creates an ACK packet
     * @param blockNumber blocknumber in the packet
     * @return
     */
    private byte[] encodeACK(short blockNumber)
    {
        byte[] ans = new byte[4];
        byte[] opCode = Util.shortToByteArray(PacketType.opCode(PacketType.ACK));
        byte[] blockInBytes = Util.shortToByteArray(blockNumber);
        ans[0] = opCode[0];
        ans[1] = opCode[1];
        ans[2] = blockInBytes[0];
        ans[3] = blockInBytes[1];
        return ans;
    }

    /**
     * saves data received from server to currentFile
     * @param data - data to save
     */
    private void saveData(byte[] data)
    {
        try
        {
            FileOutputStream writer = new FileOutputStream(currentRRQFile,true);
            writer.write(data);
            writer.flush();
            writer.close();

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
        if(data.length<512){
            finishedCommand = true;
            unSetCurrentRRQFile();
        }
    }

    /**
     * this function reads from a file and creates a packet to send to server
     * @return packet to be sent, if null we dont need to send anymore
     */
    private byte[] sendNextData()
    {
        try{
            int byteRead = fileInputStream.read(sendDataBuffer,0,512);
            byte[] encodedData;
            if(byteRead!=-1)
            {
                encodedData = encodeData(sendDataBuffer,(short)byteRead,sendDataBlockNumber);
                ++sendDataBlockNumber;
            }
            else
            {
                encodedData = encodeData(sendDataBuffer,(short)0,sendDataBlockNumber);
                unSetCurrentWRQFile();
            }
            if(byteRead<512){
                unSetCurrentWRQFile();
            }

            return encodedData;
        }
        catch (Exception e)
        {
            unSetCurrentWRQFile();
            return null;
        }

    }


    public void setCurrentRRQFile(File currentFile)
    {
        this.currentRRQFile = currentFile;
    }
    public void unSetCurrentRRQFile()
    {
        this.currentRRQFile = null;

    }
    public void setCurrentWRQFile(File currentFile,FileInputStream inputStream)
    {
        this.currentWRQFile = currentFile;
        this.fileInputStream = inputStream;
        this.sendDataBlockNumber = 1;
        this.sendDataBuffer = new byte[512];
    }
    public void unSetCurrentWRQFile()
    {
        this.currentWRQFile = null;
        try{
            this.fileInputStream.close();

        } catch (IOException ignored) {
        }
    }
    public boolean getCompletedCommand(){
        return completedCommand;
    }

    /**
     * this function decodes the message from the error
     * @param message error packet
     * @return decoded error message
     */
    private String decodeErrorMsg(byte[] message)
    {
        return new String(message,4,message.length-5, StandardCharsets.UTF_8);
    }

    /**
     * this function decodes the filename from the bcast
     * @param message bcast packet
     * @return decoded file name
     */
    private String decodeBCASTFileNime(byte[] message)
    {
        return new String(message,3,message.length-4, StandardCharsets.UTF_8);
    }
    @Override
    public boolean shouldTerminate() {
        return false;
    }

    /**
     * this function creates a command from client to server
     * @param command input from client
     * @return packet to be sent to server
     */
    public byte[] convertCommandToMessage(String[] command){
        byte[] message;
        if(command[0].equals("LOGRQ"))
        {
            if(command.length>1){
                byte[] opCode = Util.shortToByteArray(PacketType.opCode(PacketType.LOGRQ));
                message = regMessage(opCode,command);
                return message;
            }
        }
        else if(command[0].equals("DELRQ"))
        {
            if(command.length>1){
                byte[] opCode = Util.shortToByteArray(PacketType.opCode(PacketType.DELRQ));
                message = regMessage(opCode,command);
                return message;
            }
        }
        else if(command[0].equals("RRQ"))
        {
            if(command.length>1){
                byte[] opCode = Util.shortToByteArray(PacketType.opCode(PacketType.RRQ));
                message = regMessage(opCode,command);
                return message;
            }
        }
        else if(command[0].equals("WRQ"))
        {
            if(command.length>1){
                byte[] opCode = Util.shortToByteArray(PacketType.opCode(PacketType.WRQ));
                message = regMessage(opCode,command);
                return message;
            }
        }
        else if(command[0].equals("DIRQ"))
        {
            if(command.length == 1){
                return Util.shortToByteArray(PacketType.opCode(PacketType.DIRQ));
            }
        }
        else if(command[0].equals("DISC"))
        {
            if(command.length == 1){
                return Util.shortToByteArray(PacketType.opCode(PacketType.DISC));
            }
        }
        return null;// invalid command
    }

    /**
     * thsi function creates a packet the has -"command" name-
     * @param opCode opcode of the command
     * @param command input from the client
     * @return packet that can be sent to server
     */
    private byte[] regMessage(byte[] opCode,String[] command){
        String str = command[1];
        if(command.length>2){
            String[] temp = new String[command.length-1];
            for(int i = 0; i<temp.length;i++){
                temp[i] = command[i+1];
            }
            str = String.join(" ",temp);
        }
        byte[] username = str.getBytes(StandardCharsets.UTF_8);
        byte[] message = new byte[2+username.length+1];
        message[0] = opCode[0];
        message[1] = opCode[1];
        message[message.length-1] = 0;
        for(int i = 0;i< username.length ;i++){
            message[i+2] = username[i];
        }
        return message;
    }

    /**
     * return the data from inside the message
     */
    private byte[] getDataFromMsg(byte[] msg)
    {
        return Arrays.copyOfRange(msg,6,msg.length);
    }
    public boolean getFinishedCommand(){
        return finishedCommand;
    }

    /**
     *
     * @param data - buffer that holds that data to encode.
     * @param dataSize - how many bytes from the start of the buffer should be encoded.
     * @param blockNumber - current block number.
     * @return encoded data message.
     */
    public byte[] encodeData(byte[] data,short dataSize,short blockNumber)
    {
        byte[] ans = new byte[dataSize+6];
        byte[] opCode = Util.shortToByteArray(PacketType.opCode(PacketType.DATA));
        byte[] dataSizeByteArray = Util.shortToByteArray(dataSize);
        byte[] blockNumberByteArray = Util.shortToByteArray(blockNumber);
        ans[0] = opCode[0];
        ans[1] = opCode[1];
        ans[2] = dataSizeByteArray[0];
        ans[3] = dataSizeByteArray[1];
        ans[4] = blockNumberByteArray[0];
        ans[5] = blockNumberByteArray[1];
        for(int i =0;i<dataSize;i++)
        {
            ans[i+6] = data[i];
        }
        return ans;
    }

}


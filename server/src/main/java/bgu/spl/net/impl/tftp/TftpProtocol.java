package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.impl.errors.ErrorType;
import bgu.spl.net.impl.errors.Errors;
import bgu.spl.net.srv.Connections;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {
    private int connectId;
    private Connections<byte[]> connections;
    private boolean shouldTerminate;
    private boolean loggedIn;
    private Config config;
    private String fileWritingTo;
    private Random random;
    private int dataBlocksReceived;
    private FileInputStream fileInputStream;
    private FileOutputStream fileOutputStream;
    boolean isReadingFile;
    private byte[] sendDataBuffer;
    private short sendDataBlockNumber;
    private File tempDIRQFile;
    private boolean isReadingTempDIRQFile;
    private String name;
    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        this.connectId =connectionId;
        this.connections = connections;
        shouldTerminate = false;
        loggedIn = false;
        random = new Random();
        this.config = Config.getInstance();
        dataBlocksReceived = 0;
        isReadingFile = false;
        isReadingTempDIRQFile = false;
        sendDataBuffer = new byte[512];
    }

    @Override
    public void process(byte[] message) {
        if(message.length<2)
        {
            System.out.println("impossible state in process");
            throw new IllegalArgumentException();
        }
        short opCode = ( short ) ((( short ) message [0]) << 8 | ( short ) ( message [1]) );
       // System.out.println("opcode: " +opCode);
        PacketType pType = PacketType.fromOpCode(opCode);
        Errors errors = new Errors();
        boolean ackIfNoError = true;
        short ackValue = 0;
        switch (pType)
        {
            case RRQ:
                ackIfNoError = false;
                if(checkIfLoggedIn(errors))
                {
                    String fileName = decipherFileName(message);
                    File file = new File(config.fileDirectory ,fileName);
                    if(file.exists())
                    {
                        if(file.length()>0)
                        {
                            try{
                                fileInputStream = new FileInputStream(file);
                                sendDataBlockNumber = 1;
                                isReadingFile = true;
                                sendNextData(errors);
                            }
                            catch (Exception e)
                            {
                                errors.addError(ErrorType.NOT_DEFINED);
                                errors.addnNotDefinedErrMsg(e.getMessage());
                            }
                            //sendDataFromFile(file,errors);
                        }
                        else
                        {
                            errors.addError(ErrorType.ACCESS_VIOLATION);
                        }
                    }
                    else
                    {
                        errors.addError(ErrorType.FILE_NOT_FOUND);
                    }
                }
                break;
            case WRQ:
                if(checkIfLoggedIn(errors))
                {
                    String fileName = decipherFileName(message);
                    File file1 = new File(config.fileDirectory,  fileName);
                    File file2 = new File(config.tempFilesDirectory,  fileName);
                    try{
                        if(file1.exists()||!file2.createNewFile())
                        {
                            System.out.println("Got to file_already_exists");
                            errors.addError(ErrorType.FILE_ALREADY_EXISTS);
                        }
                        else
                        {
                            fileWritingTo = fileName;
                        }
                    }
                    catch (Exception e)
                    {
                        System.out.println("Got to exception");
                        errors.addError(ErrorType.NOT_DEFINED);
                        errors.addnNotDefinedErrMsg(e.getMessage());
                    }
                }
                break;
            case DATA:
                if(checkIfLoggedIn(errors)) {
                    ackValue = decodeDataBlockNumberDataPacket(message);
                    if(saveData(fileWritingTo, ackValue, getDataFromMsg(message), errors))
                    {
                        ackIfNoError = false;
                        sendErrOrAck(errors,true,ackValue);
                        connections.broadcast(encodeBCAST(true,fileWritingTo));
                    }
                }
                break;
            case DIRQ:
                ackIfNoError = false;
                if(checkIfLoggedIn(errors)) {
                    try
                    {
                        tempDIRQFile = encodeDirectoryListingToFile(errors);
                        fileInputStream = new FileInputStream(tempDIRQFile);
                        isReadingTempDIRQFile = true;
                        sendDataBlockNumber = 1;
                        if(sendNextData(errors))
                        {
                            fileInputStream.close();
                            isReadingTempDIRQFile = false;
                            tempDIRQFile.delete();
                        }
                    }
                    catch (Exception e)
                    {
                        errors.addError(ErrorType.NOT_DEFINED);
                        errors.addnNotDefinedErrMsg(e.getMessage());
                    }
                }
                break;
            case LOGRQ:
                if(loggedIn)
                {
                    errors.addError(ErrorType.USER_ALREADY_LOGGED_IN);
                }
                else
                {
                    name = decipherName(message);
                    if(connections.login(connectId,name))
                    {
                        loggedIn = true;
                    }
                    else
                    {
                        errors.addError(ErrorType.USER_ALREADY_LOGGED_IN);
                    }
                }
                break;
            case DELRQ:
                ackIfNoError = false;
                if(checkIfLoggedIn(errors))
                {
                    String fileName = decipherFileName(message);
                    File file = new File(config.fileDirectory,  fileName );
                    try {
                        if (file.exists()) {
                            if (file.length() > 0) {
                                if (!file.delete())
                                {
                                    errors.addError(ErrorType.ACCESS_VIOLATION);
                                }
                                else
                                {
                                    sendErrOrAck(errors,true,(short)0);
                                    connections.broadcast(encodeBCAST(false,fileName));
                                }
                            }
                            else {
                                errors.addError(ErrorType.ACCESS_VIOLATION);
                            }
                        }
                        else {
                            errors.addError(ErrorType.FILE_NOT_FOUND);
                        }
                    }
                    catch (Exception e)
                    {
                        errors.addError(ErrorType.NOT_DEFINED);
                        errors.addnNotDefinedErrMsg(e.getMessage());
                    }
                }

                break;
            case ACK:
                ackIfNoError = false;
                if(isReadingFile){
                    if(decodeDataBlockNumberACKPacket(message)==sendDataBlockNumber-1&&sendNextData(errors))
                    {
                        isReadingFile = false;
                        try {
                            fileInputStream.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                if(isReadingTempDIRQFile)
                {
                    if(decodeDataBlockNumberACKPacket(message)==sendDataBlockNumber-1&&sendNextData(errors))
                    {
                        isReadingTempDIRQFile = false;
                        try {
                            fileInputStream.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        tempDIRQFile.delete();
                    }
                }
                break;
            case DISC:
                if(checkIfLoggedIn(errors))
                {
                    sendErrOrAck(errors,true,ackValue);
                    connections.disconnect(connectId,name);
                }
                break;
            default:
                errors.addError(ErrorType.ILLEGAL_TFTP_OPERATION);
                break;
        }
        sendErrOrAck(errors,ackIfNoError,ackValue);
    }

    /**
     *
     * @param message LOGQR msg
     * @return username encoded to the message.
     */
    private String decipherName(byte[] message)
    {
        return new String(message,2,message.length-3);
    }
    /**
     *
     * @param message - RRQ/WRQ msg
     * @return file name encoded to the message.
     */
    private String decipherFileName(byte[] message)
    {
        return new String(message,2,message.length-3);
    }

    /**
     *
     * @return encoded error message with error type value and error msg.
     */
    private byte[] encodeError(ErrorType errorType,String errorMsg)
    {
        byte[] msgInBytes = errorMsg.getBytes(StandardCharsets.UTF_8);
        byte[] ans = new byte[msgInBytes.length+5];
        byte[] opCode = Util.shortToByteArray(PacketType.opCode(PacketType.ERROR));
        byte[] errorCode = Util.shortToByteArray((short)errorType.ordinal());
        ans[0] = opCode[0];
        ans[1] = opCode[1];
        ans[2] = errorCode[0];
        ans[3] = errorCode[1];
        for(int i =0;i<msgInBytes.length;i++)
        {
            ans[i+4] = msgInBytes[i];
        }
        ans[ans.length-1] = 0;
        return ans;
    }

    /**
     *
     * @return encoded ACK message with specified block number.
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
     *
     * @param data - buffer that holds that data to encode.
     * @param dataSize - how many bytes from the start of the buffer should be encoded.
     * @param blockNumber - current block number.
     * @return encoded data message.
     */
    private byte[] encodeData(byte[] data,short dataSize,short blockNumber)
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
    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    /**
     *
     * @param errors - Errors instance. would be updated to hold USER_NOT_LOGGED_IN if user is not logged in.
     * @return true iff user logged in.
     */
    private boolean checkIfLoggedIn(Errors errors){
        if(!loggedIn)
        {
            errors.addError(ErrorType.USER_NOT_LOGGED_IN);
            return false;
        }
        return true;
    }

    /**
     * if there is at least one error in errors, sends lowest error code to client.
     * else, if ACKIFNoError = true, sends ACK message with ackValue as blockNumber.
     */
    private void sendErrOrAck(Errors errors, boolean ACKIfNoError,short ackValue)
    {
        ErrorType err = errors.getLowestErrorCode();
        if(err==null)
        {
            if(ACKIfNoError)
            {
                connections.send(connectId,encodeACK(ackValue));
            }
        }
        else
        {
            //System.out.println("error:"  + err);
            connections.send(connectId,encodeError(err,errors.getErrorMsg()));
        }
    }

    /**
     *sends next data found in fileInputStream (max data size=512) to client.
     * @param errors - Errors instance. would be updated to hold NOT_DEFINED if an unexpected error occurred.
     * @return true iff finished sending data
     */
    private boolean sendNextData(Errors errors)
    {
        try{
            int byteRaed = fileInputStream.read(sendDataBuffer,0,512);
            if(byteRaed!=-1)
            {
                byte[] encodedData = encodeData(sendDataBuffer,(short)byteRaed,sendDataBlockNumber);
                connections.send(connectId,encodedData);
                ++sendDataBlockNumber;
            }
            else
            {
                byte[] encodedData = encodeData(sendDataBuffer,(short)0,sendDataBlockNumber);
                connections.send(connectId,encodedData);
            }
            if(byteRaed<512)
            {
                return true;
            }
        }
        catch (Exception e)
        {
            errors.addError(ErrorType.NOT_DEFINED);
            errors.addnNotDefinedErrMsg(e.getMessage());
            return true;
        }
        return false;
    }

    /**
     *
     * @param msg - data msg.
     * @return block number encoded in the message.
     */
    private short decodeDataBlockNumberDataPacket(byte[] msg)
    {
        byte[] temp = new byte[2];
        temp[0] = msg[4];
        temp[1] = msg[5];
        return Util.byteArrayToShort(temp);
    }

    /**
     *
     * @param msg - ACK msg.
     * @return block number encoded in the message.
     */
    private short decodeDataBlockNumberACKPacket(byte[] msg)
    {
        byte[] temp = new byte[2];
        temp[0] = msg[2];
        temp[1] = msg[3];
        return Util.byteArrayToShort(temp);
    }
    private byte[] getDataFromMsg(byte[] msg)
    {
        return Arrays.copyOfRange(msg,6,msg.length);
    }

    /**
     *
     * @param fileName - file name save data to.
     * @param blockNumber - current block number.
     * @param data - data to save to file.
     * @param errors - Errors instance. would be updated to hold NOT_DEFINED if an unexpected error occurred.
     * @return true iff finished saving (last client data packet received).
     */
    private boolean saveData(String fileName, short blockNumber,byte[] data,Errors errors)
    {
        try{
            File file = new File(config.tempFilesDirectory, fileName);
            if(blockNumber==1)
            {
                file.createNewFile();
                fileOutputStream = new FileOutputStream(file,true);
            }
            fileOutputStream.write(data);
            fileOutputStream.flush();
            if(data.length<512) {
                fileOutputStream.close();
                file.renameTo(new File(config.fileDirectory,fileName));
                return true;
            }
        }
        catch (Exception e)
        {
            errors.addError(ErrorType.NOT_DEFINED);
            errors.addnNotDefinedErrMsg(e.getMessage());
        }
        return false;
    }

    /**
     *
     * @param added - true=encode added, false=encoded deleted.
     * @param name - file name encode to message.
     * @return encoded BCAST message with added/deleted file name.
     */
    private byte[] encodeBCAST(boolean added,String name)
    {
        byte[] opCode = Util.shortToByteArray(PacketType.opCode(PacketType.BCAST));
        byte[] stringInUTF = name.getBytes(StandardCharsets.UTF_8);
        byte[] ans = new byte[4+stringInUTF.length];
        ans[0] = opCode[0];
        ans[1] = opCode[1];
        if(added)
        {
            ans[2] = 1;
        }
        else
        {
            ans[2] = 0;
        }
        for(int i =0;i<stringInUTF.length;i++)
        {
            ans[i+3] = stringInUTF[i];
        }
        ans[ans.length-1] = 0;
        return ans;
    }

    /**
     *
     * @param errors
     * @return
     * @throws IOException - if failed to create temp new file/failed to write to said temp file.
     */
    private File encodeDirectoryListingToFile(Errors errors) throws IOException {
        File tempFile = new File(config.tempDIRQDirectory,connectId+ "temp" + random.nextInt( 1000000) + ".txt");
        tempFile.createNewFile();
        byte[] byteArrayZero = new byte[]{0};
        FileOutputStream writer = new FileOutputStream(tempFile,true);
        File folder = config.fileDirectory;
        File[] files = folder.listFiles();
        for (int i = 0; i < files.length; i++) {
            if(files[i].length()>0) {
                byte[] FileNameInUTF8 = files[i].getName().getBytes(StandardCharsets.UTF_8);
                writer.write(FileNameInUTF8);
                writer.write(byteArrayZero);
                writer.flush();
            }
        }
        writer.close();
        return tempFile;
    }
}

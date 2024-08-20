package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.util.LinkedList;


public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    private LinkedList<Byte> bytes = new LinkedList<>();
    private short opcode = -1;
    private short dataPacketSize;
    private int packetPart = 0;
    private PacketType packetType = null;

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        boolean finishedMsg = false;
        if(packetType==null)
        {
            if(opcode==-1)
            {
                opcode = (short) (((short)nextByte)<<8);
                bytes.add(nextByte);
            }
            else
            {
                opcode = (short) (opcode|((short)nextByte));
                bytes.add(nextByte);
                packetType = PacketType.fromOpCode(opcode);
                if(packetType==null||(packetType == PacketType.DIRQ||packetType == PacketType.DISC))
                {
                    finishedMsg = true;
                }
                opcode = -1;
            }
        }
        else
        {
            bytes.add(nextByte);
            switch(packetType)
            {
                case DATA:
                    switch (packetPart) {
                        case 2:
                            dataPacketSize = (short) (((short) nextByte &0xFF) << 8);
                            break;
                        case 3:
                            dataPacketSize = (short) (dataPacketSize | ((short) nextByte)&0xFF);
                            if (dataPacketSize > 512 || dataPacketSize < 0) {
                                throw new UnsupportedOperationException();
                            }
                            break;
                        default:
                            if (packetPart  -5 == dataPacketSize ) {
                                finishedMsg = true;
                            }
                            break;
                    }
                    break;
                case ACK:
                    if(packetPart == 3){
                        finishedMsg=true;
                    }
                    break;
                case ERROR:
                    if(nextByte==0&&packetPart>3)
                    {
                        finishedMsg=true;
                    }
                    break;
                case RRQ:
                case WRQ:
                case LOGRQ:
                case DELRQ:
                    if(nextByte==0)
                    {
                        finishedMsg=true;
                    }
                    break;
                case BCAST:
                    if(nextByte==0&&packetPart>2)
                    {
                        finishedMsg=true;
                    }
                    break;
                case DIRQ:
                case DISC:
                    throw new IllegalStateException();
            }
        }
        if(finishedMsg)
        {
            packetPart = 0;
            byte[] ans = new byte[bytes.size()];
            int index  =0;
            for(Byte b : bytes)
            {
                ans[index] = b;
                ++index;
            }
            bytes.clear();
            packetType = null;
            return ans;
        }
        else
        {
            ++packetPart;
        }
        return null;
    }

    @Override
    public byte[] encode(byte[] message) {
        return message;
    }
}
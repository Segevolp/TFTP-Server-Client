package bgu.spl.net.impl.tftp;

public enum PacketType {
    RRQ,WRQ,DATA,ACK,ERROR,DIRQ,LOGRQ,DELRQ,BCAST,DISC;
    public static PacketType fromOpCode(short opCode)
    {
        switch (opCode)
        {
            case 1:
                return RRQ;
            case 2:
                return WRQ;
            case 3:
                return DATA;
            case 4:
                return ACK;
            case 5:
                return ERROR;
            case 6:
                return DIRQ;
            case 7:
                return LOGRQ;
            case 8:
                return DELRQ;
            case 9:
                return BCAST;
            case 10:
                return DISC;
        }
        throw new IllegalArgumentException();
    }
    public static short opCode(PacketType packetType)
    {
        switch (packetType)
        {
            case RRQ:
                return 1;
            case WRQ:
                return 2;
            case DATA:
                return 3;
            case ACK:
                return 4;
            case ERROR:
                return 5;
            case DIRQ:
                return 6;
            case LOGRQ:
                return 7;
            case DELRQ:
                return 8;
            case BCAST:
                return 9;
            case DISC:
                return 10;
        }
        return -1;
    }
}


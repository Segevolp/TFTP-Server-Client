package bgu.spl.net.impl.tftp;

public class Util {


    public static byte[] shortToByteArray(short s) {
        return new byte[]{(byte)(s>>8),(byte)(s & 0xff)};
    }

    public static short byteArrayToShort(byte[] bytes)
    {
        return (short)(((short)(bytes[0] & 0xFF)) << 8 | (short) (bytes[1] & 0xFF));
    }
}

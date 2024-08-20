package bgu.spl.net.impl.errors;

public enum ErrorType {
    NOT_DEFINED,FILE_NOT_FOUND,ACCESS_VIOLATION,DISK_FULL,ILLEGAL_TFTP_OPERATION,FILE_ALREADY_EXISTS,USER_NOT_LOGGED_IN,
    USER_ALREADY_LOGGED_IN;

    @Override
    public String toString()
    {
        switch (this)
        {
            case NOT_DEFINED:
                return "Not defined.";
            case FILE_NOT_FOUND:
                return "File not found.";
            case ACCESS_VIOLATION:
                return "Access violation - File cannot be written, read or deleted.";
            case DISK_FULL:
                return "Disk full or allocation exceeded - No room in disk.";
            case USER_NOT_LOGGED_IN:
                return "User not logged in.";
            case FILE_ALREADY_EXISTS:
                return "File already exists.";
            case ILLEGAL_TFTP_OPERATION:
                return "Illegal TFTP operation - Unknown Opcode.";
            case USER_ALREADY_LOGGED_IN:
                return "User already logged in.";
        }
        return null;
    }
}

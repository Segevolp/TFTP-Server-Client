package bgu.spl.net.impl.errors;

/**
 * this class is used to store all accumulated errors while processing a message
 * and later get the lowest error code it accumulated.
 */
public class Errors {
    private boolean[] errors;
    private String notDefinedErrMsg;
    public Errors()
    {
        errors = new boolean[ErrorType.values().length];
    }

    /**
     *
     * @param errorType errorType to add to accumulated errors.
     */
    public void addError(ErrorType errorType)
    {
        errors[errorType.ordinal()] = true;
    }

    /**
     * sets notDefinedErrMsg to given msg.
     */
    public void addnNotDefinedErrMsg(String msg)
    {
        this.notDefinedErrMsg = msg;
    }

    /**
     *
     * @return error msg of the lowest error code accumulated. if its NOT_DEFINED - returns notDefinedErrMsg.
     */
    public String getErrorMsg()
    {
        ErrorType err = getLowestErrorCode();
        if(err!=null)
        {
            if(err == ErrorType.NOT_DEFINED)
            {
                return notDefinedErrMsg;
            }
            return err.toString();
        }
        return null;
        //throw new Exception();
    }

    /**
     *
     * @return lowest error type or null of none.
     */
    public ErrorType getLowestErrorCode()
    {
        for(int i =0;i<errors.length;i++)
        {
            if(errors[i])
            {
                return ErrorType.values()[i];
            }
        }
        return null;
    }
}

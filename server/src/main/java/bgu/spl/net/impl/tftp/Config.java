package bgu.spl.net.impl.tftp;

import java.io.File;

public class Config {
    private static Config config = new Config();
    /**
     * directory where files in server should be stored.
     */
    public final File fileDirectory;
    /**
     * directory where temporary files in server (files that are in upload state) should be stored.
     */
    public final File tempFilesDirectory;
    /**
     * directory where temp DIRQ files in server (temporary files used to execute DIRQ, which being deleted
     * after completion) should be stored.
     */
    public final File tempDIRQDirectory;
    public static Config getInstance(){return config;}


    private Config()
    {
        File mainDirectory = new File(System.getProperty("user.dir"));
        fileDirectory = new File(mainDirectory,"Files");
        tempFilesDirectory = new File(mainDirectory,"TempFiles");
        tempDIRQDirectory = new File(mainDirectory,"TempDIRQ");
        try
        {
            if(!fileDirectory.exists())
            {
                fileDirectory.mkdir();
            }
            if(!tempFilesDirectory.exists())
            {
                tempFilesDirectory.mkdir();
            }
            if(!tempDIRQDirectory.exists())
            {
                tempDIRQDirectory.mkdir();
            }
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
}

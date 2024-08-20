package bgu.spl.net.impl.tftp;

import java.io.File;

public class Config {
    private static Config config = new Config();

    public final File fileDirectory;
    public static Config getInstance(){return config;}


    private Config()
    {
        File mainDirectory = new File(System.getProperty("user.dir"));
        //File mainDirectory = new File("D:\\degree\\SPL\\SPL3\\client");
        fileDirectory = new File(mainDirectory,"");
        try
        {
            if(!fileDirectory.exists())
            {
                fileDirectory.mkdir();
            }
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
}

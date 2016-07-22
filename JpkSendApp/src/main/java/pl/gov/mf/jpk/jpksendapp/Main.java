package pl.gov.mf.jpk.jpksendapp;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import pl.gov.mf.jpk.jpksendapp.http.RestClient;

public class Main
{
    public static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    
    //public static final String LINE = "--------------------------------------------------------------------------------";

    public static String APP_NAME;
    public static String APP_TITLE;
    public static String APP_VERSION;
    
    public static ReleaseMode APP_RELEASE_MODE;
    public static ReleaseLevel APP_RELEASE_LEVEL;
    
    public static File appDir = null;
    public static File appHome = null;
    
    public static FileHandler fileHandler = null;
    public static FileHandler dailyHandler = null;
    
    public static void main(String[] args)
    {
        Main.init();
    }
    
    public static void init()
    {
        try
        {
            System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT [%4$-7s] %5$s%6$s%n");
            
            Properties properties = new Properties();
            
            properties.load(Main.class.getResourceAsStream("resources/application.properties"));
            
            APP_NAME = properties.getProperty("app.name");
            APP_TITLE = properties.getProperty("app.title");
            APP_VERSION = properties.getProperty("app.version");
            
            String appReleaseMode = properties.getProperty("app.release.mode");
            
            if (ReleaseMode.PRD.name().equals(appReleaseMode))
            {
                APP_RELEASE_MODE = ReleaseMode.PRD;
            }
            else
            {
                APP_RELEASE_MODE = ReleaseMode.TST;
            }
            
            String appReleaseLevel = properties.getProperty("app.release.level");
            
            if (ReleaseLevel.PRD.name().equals(appReleaseLevel))
            {
                APP_RELEASE_LEVEL = ReleaseLevel.PRD;
            }
            else
            {
                APP_RELEASE_LEVEL = ReleaseLevel.TST;
            }
            
            if (APP_RELEASE_LEVEL == ReleaseLevel.TST)
            {
                APP_RELEASE_MODE = ReleaseMode.TST;
            }
            
            appDir = new File(System.getProperty("user.home") + File.separator + "." + APP_NAME);
            
            if (!appDir.exists())
            {
                if (!appDir.mkdir())
                {
                    LOGGER.log(Level.SEVERE, "Cannot create directory {0}", appDir.getAbsolutePath());
                }
            }
            
            appHome = new File(appDir.getAbsolutePath() + File.separator + APP_VERSION);
            
            if (!appHome.exists())
            {
                if (!appHome.mkdir())
                {
                    LOGGER.log(Level.SEVERE, "Cannot create directory {0}", appHome.getAbsolutePath());
                }
            }
            
            dailyHandler = new FileHandler(appHome + File.separator + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".log", true);
        
            dailyHandler.setFormatter(new SimpleFormatter());

            Main.configureLogger(Main.LOGGER);
            
            Main.configureLogger(WinApp.LOGGER);
            
            Main.configureLogger(RestClient.LOGGER);
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
    
    private static void configureLogger(Logger logger)
    {
        for (Handler handler: logger.getHandlers())
        {
            logger.removeHandler(handler);
            
            handler.close();
        }

        logger.addHandler(dailyHandler);

        logger.setLevel(Level.ALL);
    }
    
    public static void closeHandlers()
    {
        if (Main.fileHandler != null)
        {
            Main.fileHandler.close();
        }
        
        if (Main.dailyHandler != null)
        {
            Main.dailyHandler.close();
        }
    }
    
    public enum ReleaseMode
    {
        TST, PRD
    }
    public enum ReleaseLevel
    {
        TST, PRD
    }
}

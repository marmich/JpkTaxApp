package pl.gov.mf.jpk.jpkinitapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.apache.commons.lang3.RandomStringUtils;
import pl.gov.mf.jpk.jpkinitapp.util.AesUtil;
import pl.gov.mf.jpk.jpkinitapp.util.DigestUtil;
import pl.gov.mf.jpk.jpkinitapp.util.JaxbUtil;
import pl.gov.mf.jpk.jpkinitapp.util.RsaUtil;
import pl.gov.mf.jpk.jpkinitapp.util.SplitUtil;
import pl.gov.mf.jpk.jpkinitapp.util.XmlUtil;
import pl.gov.mf.jpk.jpkinitapp.util.Zip4jUtil;


public class Main
{
    public static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    
    public static final String LINE = "----------------------------------------------------------------------------------------------";
    
    public static final String AES_FILE_DOT_EXT = ".aes";
    public static final String JPK_FILE_DOT_EXT = ".xml";
    
    public static String APP_NAME;
    public static String APP_TITLE;
    public static String APP_VERSION;
    
    public static ReleaseMode APP_RELEASE_MODE;
    public static ReleaseLevel APP_RELEASE_LEVEL;
    
    public static File appDir = null;
    public static File appHome = null;
    public static File appLocus = null;
    
    public static FileHandler fileHandler = null;
    public static FileHandler dailyHandler = null;
    
    public static boolean APP_DEBUG = false;
    public static boolean XML_VALIDATE = true;
    
    public static boolean COMPLETED = false;
    
    public static byte SPLIT_FILE_SIZE_MIN = 60;
    public static final byte SPLIT_FILE_SIZE_MAX = 60;
    
    public static void main(String[] args)
    {
        Main.init();
        
        File xmlFile = null;

        ArrayList<File> xmlFiles = new ArrayList();

        try
        {
            System.out.println(Main.LINE);
            System.out.println(Main.APP_NAME + " application launched ...");

            if ((args.length == 0) || (args[0] == null))
            {
                System.out.println("JPK file name is required!");

                return;
            }
            else
            {
                xmlFile = new File(args[0]);

                if (!xmlFile.exists())
                {
                    System.out.println("Cannot find file " + args[0]);

                    return;
                }

                if (xmlFile.isFile())
                {
                    xmlFiles.add(xmlFile);
                }
                else if (xmlFile.isDirectory())
                {
                    FilenameFilter filter = (File directory, String filename) -> {
                        return filename.endsWith(JPK_FILE_DOT_EXT);
                    };
                    
                    File[] files = xmlFile.listFiles(filter);

                    for (File file : files)
                    {
                        if (file.isFile())
                        {
                            xmlFiles.add(file);
                        }
                    }
                }
            }
            
            if ((args.length > 1) && (args[1] != null))
            {
                File cfgFile = new File(args[1]);
                
                if (cfgFile.exists() && cfgFile.isFile())
                {
                    Properties properties = new Properties();
                    
                    properties.load(new FileInputStream(cfgFile));

                    APP_DEBUG = Boolean.valueOf(properties.getProperty("app.debug"));
                    
                    XML_VALIDATE = Boolean.valueOf(properties.getProperty("xml.validate"));

                    SPLIT_FILE_SIZE_MIN = Byte.valueOf(properties.getProperty("split.file.size.min"));
                }
            }
            
            for (File jpkFile: xmlFiles)
            {
                Main.perform(jpkFile);
            }
            
            System.out.println(Main.LINE);
            System.out.println(Main.APP_NAME + " application finished!");
            System.out.println(Main.LINE);
        }
        catch (Exception ex)
        {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public static void writeSize(long size)
    {
        System.out.print("Size: " + size + " bytes ");

        if (size >= 1024 * 1024 * 1024)
        {
            System.out.println("(" + size / 1024 / 1024 / 1024 + "GB)");
        }
        else if (size >= 1024 * 1024)
        {
            System.out.println("(" + size / 1024 / 1024 + "MB)");
        }
        else if (size >= 1024)
        {
            System.out.println("(" + size / 1024 + "kB)");
        }
    }

    public static void writeTime(long starttime)
    {
        long finishtime = System.currentTimeMillis() - starttime;

        System.out.print("Execution time ");

        if (finishtime < 1000)
        {
            System.out.println((finishtime + 1) + " miliseconds");
        }
        else if (finishtime < 1000 * 60)
        {
            System.out.println((finishtime / 1000 + 1) + " seconds");
        }
        else
        {
            System.out.println((finishtime / 1000 / 60 + 1) + " minutes");
        }

        System.out.println(Main.LINE);
    }

    public static void printSize(long size)
    {
        String value = "Size: " + size + " bytes ";

        if (size >= 1024 * 1024 * 1024)
        {
            value += "(" + size / 1024 / 1024 / 1024 + "GB)";
        }
        else if (size >= 1024 * 1024)
        {
            value += "(" + size / 1024 / 1024 + "MB)";
        }
        else if (size >= 1024)
        {
            value += "(" + size / 1024 + "kB)";
        }
        
        Main.LOGGER.log(Level.FINE, value);
    }

    public static void printTime(long starttime)
    {
        long finishtime = System.currentTimeMillis() - starttime;

        String value = "Execution time ";

        if (finishtime < 1000)
        {
            value += (finishtime + 1) + " miliseconds";
        }
        else if (finishtime < 1000 * 60)
        {
            value += (finishtime / 1000 + 1) + " seconds";
        }
        else
        {
            value += (finishtime / 1000 / 60 + 1) + " minutes";
        }

        Main.LOGGER.log(Level.FINE, value);
        Main.LOGGER.log(Level.FINE, Main.LINE);
    }
    
    public static synchronized void perform(File jpkFile)
    {
        XmlUtil xmlUtil = null;
        RsaUtil rsaUtil = null;
        AesUtil aesUtil = null;
        JaxbUtil jaxbUtil = null;
        Zip4jUtil zipComp = null;
        SplitUtil splitFiles = null;
        DigestUtil digestJpkSha = null;
        
        Main.COMPLETED = false;
        
        try
        {
            if (Main.fileHandler != null)
            {
                for (Handler handler: Main.LOGGER.getHandlers())
                {
                    if (Main.fileHandler == handler)
                    {
                        Main.LOGGER.removeHandler(handler);

                        handler.close();
                    }
                }
            }
            
            Main.LOGGER.log(Level.INFO, "Sprawdzanie poprawności struktury XML\n");
            
            xmlUtil = new XmlUtil(jpkFile);

            xmlUtil.extractJpkHeader();
            
            if (XML_VALIDATE)
            {
                if (!xmlUtil.isCorrectHeader())
                {
                    Main.LOGGER.log(Level.INFO, "Nieznana struktura XML!\n");
                    
                    return;
                }
                else if (!xmlUtil.isSupportedCode())
                {
                    Main.LOGGER.log(Level.INFO, "Nieobsługiwany rodzaj dokumentu!\n");
                    
                    return;
                }
                else if (!xmlUtil.isValidDocument())
                {
                    Main.LOGGER.log(Level.INFO, "Niepoprawna struktura dokumentu!\n");
                    
                    return;
                }
                else
                {
                    Main.LOGGER.log(Level.INFO, "Przetwarzanie dokumentu {0}: {1}\n", new String[]{xmlUtil.getFormCode(), jpkFile.getName()});
                }
            }
            else
            {
                Main.LOGGER.log(Level.INFO, "Przetwarzanie dokumentu: {1}\n", jpkFile.getName());
            }

            File temp = new File(jpkFile.getParent() + File.separator + jpkFile.getName().substring(0, jpkFile.getName().length() - JPK_FILE_DOT_EXT.length()));
            File moved = new File(temp.getAbsolutePath() + File.separator + jpkFile.getName());

            if (temp.exists())
            {
                Main.LOGGER.log(Level.SEVERE, "Katalog już istnieje: {0}\n", temp.getAbsolutePath());

                return;
            }
            else
            {
                if (temp.mkdir())
                {
                    if (!jpkFile.renameTo(moved))
                    {
                        Main.LOGGER.log(Level.SEVERE, "Nie można przenieść pliku do: {0}\n", moved.getAbsolutePath());

                        return;
                    }
                }
                else
                {
                    Main.LOGGER.log(Level.INFO, "Nie można utworzyć katalogu: {0}\n", temp.getAbsolutePath());

                    return;
                }
            }

            jpkFile = moved;

            int dotIndex;

            if ((dotIndex = jpkFile.getAbsolutePath().lastIndexOf(".")) != -1)
            {
                Main.fileHandler = new FileHandler(jpkFile.getAbsolutePath().substring(0, dotIndex) + ".log", true);
            }
            else
            {
                Main.fileHandler = new FileHandler(jpkFile.getAbsolutePath() + ".log", true);
            }

            Main.fileHandler.setFormatter(new SimpleFormatter());

            Main.LOGGER.addHandler(Main.fileHandler);

            Main.LOGGER.setLevel(Level.ALL);

            Main.LOGGER.log(Level.FINE, "File: {0}", jpkFile);

            Main.printSize(jpkFile.length());

            Main.LOGGER.log(Level.FINE, Main.LINE);

            Main.LOGGER.log(Level.INFO, "Obliczanie wartości skrótu dokumentu\n");
            
            digestJpkSha = new DigestUtil(jpkFile);

            digestJpkSha.sha256File();

            Main.LOGGER.log(Level.INFO, "Tworzenie archiwum w formacie ZIP\n");
            
            zipComp = new Zip4jUtil(new File(jpkFile.getAbsolutePath() + ".zip"));

            ArrayList<File> jpkFiles = new ArrayList();

            jpkFiles.add(jpkFile);

            zipComp.createZipFile(jpkFiles);

            int zipSizeMB = (int) (zipComp.getSize() / (1024 * 1024));

            if (zipSizeMB != 0)
            {
                if ((zipComp.getSize() % (1024 * 1024)) != 0)
                {
                    zipSizeMB++;
                }
            }

            if (SPLIT_FILE_SIZE_MIN < 1)
            {
                SPLIT_FILE_SIZE_MIN = 1;
            }

            if (SPLIT_FILE_SIZE_MIN > SPLIT_FILE_SIZE_MAX)
            {
                SPLIT_FILE_SIZE_MIN = SPLIT_FILE_SIZE_MAX;
            }

            if (zipSizeMB > SPLIT_FILE_SIZE_MIN)
            {
                Scanner scanner = null;

                try
                {
                    int size = SPLIT_FILE_SIZE_MIN;

                    if ((SPLIT_FILE_SIZE_MIN != SPLIT_FILE_SIZE_MAX) && (SPLIT_FILE_SIZE_MIN < (zipSizeMB - 1)))
                    {
                        System.out.print("Enter split size in MB (" + SPLIT_FILE_SIZE_MIN + "-" + (zipSizeMB - 1) + "): ");

                        scanner = new Scanner(System.in);

                        size = scanner.nextInt();
                    }

                    if (size > (zipSizeMB - 1))
                    {
                        System.out.println("The specified split size is too large!");

                        return;
                    }
                    else
                    {
                        Main.LOGGER.log(Level.INFO, "Dzielenie archiwum na części binarne\n");
                        
                        splitFiles = new SplitUtil(zipComp.getFile(), size);

                        splitFiles.splitFile();

                        if (!APP_DEBUG)
                        {
                            if (zipComp.getFile().delete())
                            {
                                Main.LOGGER.log(Level.FINE, "Successfully delete file: {0}", zipComp.getFile().getAbsolutePath());
                            }
                            else
                            {
                                Main.LOGGER.log(Level.WARNING, "Cannot delete file: {0}", zipComp.getFile().getAbsolutePath());
                            }

                            Main.LOGGER.log(Level.FINE, Main.LINE);
                        }
                    }
                }
                catch (Exception ex)
                {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                }
                finally
                {
                    if (scanner != null)
                    {
                        scanner.close();
                    }
                }
            }

            Main.LOGGER.log(Level.INFO, "Generowanie losowych danych szyfrowania\n");
            aesUtil = new AesUtil(RandomStringUtils.randomAlphanumeric(32).getBytes(), RandomStringUtils.randomAlphanumeric(16).getBytes());
            //aesUtil = new AesUtil("12345678901234567890123456789012".getBytes(), "1234567890123456".getBytes());

            Main.LOGGER.log(Level.INFO, "Klucz: {0}", new String(aesUtil.getKey()));
            Main.LOGGER.log(Level.INFO, "Wektor: {0}\n", new String(aesUtil.getVec()));
            
            Main.LOGGER.log(Level.INFO, "Szyfrowanie archiwum\n");
                
            if ((splitFiles != null) && (!splitFiles.getFiles().isEmpty()))
            {
                int index = 0;
                int count = splitFiles.getFiles().size();
                
                for (File file: splitFiles.getFiles())
                {
                    Main.LOGGER.log(Level.INFO, "Szyfrowanie części {0} z {1}\n", new Integer[]{ ++index, count});

                    aesUtil.encryptFile(file);

                    if (!APP_DEBUG)
                    {
                        if (file.delete())
                        {
                            Main.LOGGER.log(Level.FINE, "Successfully delete file: {0}", file.getAbsolutePath());
                        }
                        else
                        {
                            Main.LOGGER.log(Level.WARNING, "Cannot delete file: {0}", file.getAbsolutePath());
                        }

                        Main.LOGGER.log(Level.FINE, Main.LINE);
                    }
                }
            }
            else
            {
                aesUtil.encryptFile(zipComp.getFile());

                if (!APP_DEBUG)
                {
                    if (zipComp.getFile().delete())
                    {
                        Main.LOGGER.log(Level.FINE, "Successfully delete file: {0}", zipComp.getFile().getAbsolutePath());
                    }
                    else
                    {
                        Main.LOGGER.log(Level.WARNING, "Cannot delete file: {0}", zipComp.getFile().getAbsolutePath());
                    }

                    Main.LOGGER.log(Level.FINE, Main.LINE);
                }
            }

            Main.LOGGER.log(Level.INFO, "Szyfrowanie klucza szyfrującego\n");

            if (ReleaseMode.PRD == Main.APP_RELEASE_MODE)
            {
                rsaUtil = new RsaUtil(Main.class.getResourceAsStream("/pl/gov/mf/jpk/jpkinitapp/resources/prd/3af5843ae11db6d94edf0ea502b5cd1a.crt"));
            }
            else
            {
                rsaUtil = new RsaUtil(Main.class.getResourceAsStream("/pl/gov/mf/jpk/jpkinitapp/resources/tst/356e5db89ef1b6f9b0779a8e6e64dd96.crt"));
            }

            rsaUtil.encrypt(aesUtil.getKey());

            jaxbUtil = new JaxbUtil(Main.APP_RELEASE_MODE, jpkFile, xmlUtil, rsaUtil, aesUtil, digestJpkSha);
            
            Main.LOGGER.log(Level.INFO, "Generowanie pliku metadanych\n");

            jaxbUtil.create();

            jaxbUtil.marshal(XML_VALIDATE);

            if (Main.COMPLETED)
            {
                Main.LOGGER.log(Level.INFO, "Poprawnie zakończono przetwarzanie dokumentu.");
            }
            else
            {
                Main.LOGGER.log(Level.INFO, "Błędne przetwarzanio dokumentu!");
            }
            
            Main.LOGGER.log(Level.INFO, Main.LINE);
        }
        catch (SecurityException | IOException ex)
        {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
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
            
            APP_DEBUG = Boolean.valueOf(properties.getProperty("app.debug"));
            
            XML_VALIDATE = Boolean.valueOf(properties.getProperty("xml.validate"));
            
            SPLIT_FILE_SIZE_MIN = Byte.valueOf(properties.getProperty("split.file.size.min"));
            
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
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Cannot create directory {0}", appDir.getAbsolutePath());
                }
            }
            
            appHome = new File(appDir.getAbsolutePath() + File.separator + APP_VERSION);
            
            if (!appHome.exists())
            {
                if (!appHome.mkdir())
                {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Cannot create directory {0}", appHome.getAbsolutePath());
                }
            }
            
            dailyHandler = new FileHandler(appHome + File.separator + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".log", true);
        
            dailyHandler.setFormatter(new SimpleFormatter());

            Main.configureLogger(Main.LOGGER);
            
            Main.configureLogger(WinApp.LOGGER);
            
            CodeSource codeSource = Main.class.getProtectionDomain().getCodeSource();
            
            File jarFile = new File(codeSource.getLocation().toURI().getPath());
            
            appLocus = jarFile.getParentFile();
        }
        catch (IOException | URISyntaxException ex)
        {
            Logger.getLogger(WinApp.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
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

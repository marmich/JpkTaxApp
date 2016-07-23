package pl.gov.mf.jpk.jpksignapp;

import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.DSSException;
import eu.europa.esig.dss.FileDocument;
import eu.europa.esig.dss.SignatureLevel;
import eu.europa.esig.dss.SignaturePackaging;
import eu.europa.esig.dss.SignatureValue;
import eu.europa.esig.dss.ToBeSigned;
import eu.europa.esig.dss.token.JKSSignatureToken;
import eu.europa.esig.dss.token.KSPrivateKeyEntry;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.signature.XAdESService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.apache.commons.io.IOUtils;
import pl.gov.mf.jpk.jpksignapp.util.JKSTool;


public class Main
{
    private static final String LINE = "--------------------------------------------------------------------------------";

    public static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static String APP_NAME;
    public static String APP_TITLE;
    public static String APP_VERSION;
    
    public static File appDir = null;
    public static File appHome = null;
    
    public static FileHandler fileHandler = null;
    public static FileHandler dailyHandler = null;
    
    public static void main(String[] args)
    {
        Main.init();
        
        File file;

        ArrayList<File> files = new ArrayList();
        
        if ((args.length == 0) || (args[0] == null))
        {
            LOGGER.log(Level.SEVERE, "Any file as argument is required!");

            return;
        }
        else
        {
            file = new File(args[0]);

            if (!file.exists())
            {
                file = new File(System.getProperty("user.dir") + File.separator + args[0]);

                if (!file.exists())
                {
                    LOGGER.log(Level.SEVERE, "Cannot find file {0}", args[0]);

                    return;
                }
            }

            if (file.isFile())
            {
                files.add(file);
            }
            else if (file.isDirectory())
            {
                for (File child: file.listFiles())
                {
                    if (child.isFile())
                    {
                        files.add(child);
                    }
                }
            }
        }

        for (File item: files)
        {
            new Main().signXmlDocumentJks(item);
        }
    }

    public void signXmlDocumentJks(File file)
    {
        InputStream jksFile = null;
        FileOutputStream xadesFile = null;
        
        String password =  "password";
        String alias = "jpk";
        String key = "jpkkey";

        try
        {
            LOGGER.log(Level.INFO, "Processing file: {0}\n", file.getAbsolutePath());
            
            jksFile = Main.class.getResourceAsStream("resources/jpk.jks");

            JKSSignatureToken signingToken = new JKSSignatureToken(jksFile, password);

            JKSTool jks = new JKSTool(Main.class.getResource("resources/jpk.jks"), password.toCharArray());

            KSPrivateKeyEntry privateKey = jks.getPrivateKey(alias, key);

            DSSDocument toBeSigned = new FileDocument(file);

            XAdESSignatureParameters params = new XAdESSignatureParameters();

            params.setSignatureLevel(SignatureLevel.XAdES_BASELINE_B);
            params.setSignaturePackaging(SignaturePackaging.ENVELOPING);
            params.setSigningCertificate(privateKey.getCertificate());
            params.setCertificateChain(privateKey.getCertificateChain());
            params.bLevel().setSigningDate(new Date());

            //Default digest algorithm - SHA256
            //params.setDigestAlgorithm(DigestAlgorithm.SHA256);
            //Default encryption algorithm - RSA/ECB/PKCS1Padding
            //params.setEncryptionAlgorithm(EncryptionAlgorithm.RSA);
            CommonCertificateVerifier commonCertificateVerifier = new CommonCertificateVerifier();

            XAdESService service = new XAdESService(commonCertificateVerifier);

            ToBeSigned dataToSign = service.getDataToSign(toBeSigned, params);
            SignatureValue signatureValue = signingToken.sign(dataToSign, params.getDigestAlgorithm(), privateKey);
            DSSDocument signedDocument = service.signDocument(toBeSigned, params, signatureValue);

            int dotIndex;

            if ((dotIndex = file.getAbsolutePath().lastIndexOf(".")) != -1)
            {
                xadesFile = new FileOutputStream(file.getAbsolutePath().substring(0, dotIndex) + ".xades");
            }
            else
            {
                xadesFile = new FileOutputStream(file.getAbsolutePath() + ".xades");
            }

            IOUtils.copy(signedDocument.openStream(), xadesFile);
            
            LOGGER.log(Level.INFO, "Successfully finished!");
        }
        catch (NullPointerException | DSSException | IOException ex)
        {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
        finally
        {
            if (xadesFile != null)
            {
                try
                {
                    xadesFile.close();
                }
                catch (IOException ex)
                {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                }
            }

            if (jksFile != null)
            {
                try
                {
                    jksFile.close();
                }
                catch (IOException ex)
                {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
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
}

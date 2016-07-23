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
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import pl.gov.mf.jpk.jpksignapp.util.JKSTool;


public class Main
{
    private static final String LINE = "--------------------------------------------------------------------------------";

    public static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args)
    {
        File file;

        ArrayList<File> files = new ArrayList();

        if ((args.length == 0) || (args[0] == null))
        {
            System.out.println("XML file name is required!");

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
                    System.out.println("Cannot find file " + args[0]);

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

    //public void signXmlDocumentJks(File file, URL url, String key, String alias, String pwd)
    public void signXmlDocumentJks(File file)
    {
        InputStream jksFile = null;
        FileOutputStream xadesFile = null;
        
        String password =  "password";
        String alias = "jpk";
        String key = "jpkkey";

        try
        {
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
        }
        catch (NullPointerException | DSSException | IOException ex)
        {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
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
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
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
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}

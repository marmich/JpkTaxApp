package pl.gov.mf.jpk.jpkinitapp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Level;
import javax.crypto.Cipher;
import org.apache.commons.codec.binary.Base64;
import pl.gov.mf.jpk.jpkinitapp.Main;


public class RsaUtil
{
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    private static final String ALGORITHM = "RSA";
    
    private byte[] encryptedKey = null;
    private PublicKey publicKey = null;

    private RsaUtil()
    {
    }

    public RsaUtil(File crtFile)
    {
        this.publicKey = this.getCrtPublicKey(crtFile);
    }
    
    public RsaUtil(InputStream crtStream)
    {
        this.publicKey = this.getCrtPublicKey(crtStream);
    }
    
    public RsaUtil(X509Certificate cert)
    {
        this.publicKey = cert.getPublicKey();
    }
    
    public PublicKey getPublicKey()
    {
        return publicKey;
    }
    
    public byte[] getEncryptedKey()
    {
        return encryptedKey;
    }

    public String getEncryptedKeyHex()
    {
        String value = "";
        
        for (int i = 0; i < encryptedKey.length; i++)
        {
            value += String.format("%02x", encryptedKey[i]);
        }
        
        return value;
    }
    
    public String getEncryptedKeyBase64()
    {
        return Base64.encodeBase64String(encryptedKey);
    }
    
    public void encrypt(byte[] secretKey)
    {
        Main.LOGGER.log(Level.FINE, "{0} method encrypt", this.getClass().getSimpleName());
        
        long starttime = System.currentTimeMillis();
        
        try
        {
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance(ALGORITHM).generatePublic(x509EncodedKeySpec));

            encryptedKey = cipher.doFinal(secretKey);
        }
        catch (GeneralSecurityException ex)
        {
            Main.LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        
        Main.printTime(starttime);
    }

    private PublicKey getCrtPublicKey(File crtFile)
    {
        PublicKey publicKey = null;

        FileInputStream stream = null;
        
        try
        {
            stream = new FileInputStream(crtFile);

            publicKey = this.getCrtPublicKey(stream);
        }
        catch (FileNotFoundException ex)
        {
            Main.LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        finally
        {
            if (stream != null)
            {
                try
                {
                    stream.close();
                }
                catch (IOException ex)
                {
                    Main.LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }

        return publicKey;
    }
    
    private PublicKey getCrtPublicKey(InputStream stream)
    {
        PublicKey publicKey = null;

        try
        {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");

            Certificate cert = factory.generateCertificate(stream);

            publicKey = cert.getPublicKey();
        }
        catch (CertificateException ex)
        {
            Main.LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        finally
        {
            if (stream != null)
            {
                try
                {
                    stream.close();
                }
                catch (IOException ex)
                {
                    Main.LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }

        return publicKey;
    }
}

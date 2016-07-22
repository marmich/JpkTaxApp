package pl.gov.mf.jpk.jpkinitapp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import pl.gov.mf.jpk.jpkinitapp.Main;


public class DigestUtil
{
    private byte[] digest;
    
    private File file;

    private DigestUtil()
    {
    }
    
    public DigestUtil(String path)
    {
        this(new File(path));
    }
    
    public DigestUtil(File file)
    {
        this.file = file;
    }
    
    public byte[] getDigest()
    {
        return digest;
    }
    
    public String getDigestHex()
    {
        String value = "";
        
        for (int i = 0; i < digest.length; i++)
        {
            value += String.format("%02x", digest[i]);
        }
        
        return value;
    }
    
    public String getDigestBase64()
    {
        return Base64.encodeBase64String(digest);
    }
    
    public void md5File()
    {
        Main.LOGGER.log(Level.FINE, "{0} method md5File", this.getClass().getSimpleName());

        long starttime = System.currentTimeMillis();
        
        FileInputStream stream = null;
        
        try
        {
            Main.LOGGER.log(Level.FINE, "File: {0}", file.getAbsolutePath());
            
            stream = new FileInputStream(file);
            
            digest = DigestUtils.md5(stream);
            
            Main.LOGGER.log(Level.FINE, "MD5 hex: {0}", getDigestHex());
            
            Main.LOGGER.log(Level.FINE, "MD5 Base64: {0}", getDigestBase64());
        }
        catch (IOException ex)
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
        
        Main.printTime(starttime);
    }
    
    public void sha256File()
    {
        Main.LOGGER.log(Level.FINE, "{0} method sha256File", this.getClass().getSimpleName());

        long starttime = System.currentTimeMillis();
        
        FileInputStream stream = null;
        
        try
        {
            Main.LOGGER.log(Level.FINE, "File: {0}", file.getAbsolutePath());
            
            stream = new FileInputStream(file);
            
            digest = DigestUtils.sha256(stream);
            
            Main.LOGGER.log(Level.FINE, "SHA256 hex: {0}", getDigestHex());
            
            Main.LOGGER.log(Level.FINE, "SHA256 Base64: {0}", getDigestBase64());
        }
        catch (IOException ex)
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
        
        Main.printTime(starttime);
    }
}

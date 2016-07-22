package pl.gov.mf.jpk.jpkinitapp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.logging.Level;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import pl.gov.mf.jpk.jpkinitapp.Main;


public class AesUtil
{
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String ALGORITHM = "AES";
    
    private ArrayList<File> aesFiles = new ArrayList();
    
    private byte[] key;
    private byte[] vec;
    
    private AesUtil()
    {
    }
        
    public AesUtil(byte[] key, byte[] vec)
    {
        this.key = key;
        this.vec = vec;
    }

    public byte[] getKey()
    {
        return key;
    }

    public byte[] getVec()
    {
        return vec;
    }

    public String getVecHex()
    {
        String value = "";
        
        for (int i = 0; i < this.vec.length; i++)
        {
            value += String.format("%02x", this.vec[i]);
        }
        
        return value;
    }
    
    public String getVecBase64()
    {
        return Base64.encodeBase64String(this.vec);
    }
    
    public ArrayList<File> getAesFiles()
    {
        return aesFiles;
    }
    
    public void encryptFile(String file)
    {
        this.encryptFile(new File(file));
    }
    
    public void encryptFile(File file)
    {
        Main.LOGGER.log(Level.FINE, "{0} method encryptFile", this.getClass().getSimpleName());
        
        long starttime = System.currentTimeMillis();
        
        File aesFile = new File(file.getAbsolutePath() + Main.AES_FILE_DOT_EXT);

        Main.LOGGER.log(Level.FINE, "AES file: {0}", aesFile.getAbsolutePath());
        
        this.aesFiles.add(aesFile);
        
        FileInputStream fis = null;
        FileOutputStream fos = null;

        try
        {
            SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM);

            IvParameterSpec ivParameter = new IvParameterSpec(vec);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameter);
            
            fis = new FileInputStream(file);

            fos = new FileOutputStream(aesFile);
            
            int length;
            byte[] input = new byte[16];
            
            long i = file.length()/input.length;
            
            if ((file.length()%input.length) != 0)
            {
                i++;
            }
            
            while ((length = fis.read(input)) != -1)
            {
                if (--i != 0)
                {
                    fos.write(cipher.update(input));
                }
                else
                {
                    fos.write(cipher.doFinal(input, 0, length));
                }
            }
        }
        catch (GeneralSecurityException | IOException ex)
        {
            Main.LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        finally
        {
            if (fis != null)
            {
                try
                {
                    fis.close();
                }
                catch (IOException ex)
                {
                    Main.LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
            
            if (fos != null)
            {
                try
                {
                    fos.close();
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

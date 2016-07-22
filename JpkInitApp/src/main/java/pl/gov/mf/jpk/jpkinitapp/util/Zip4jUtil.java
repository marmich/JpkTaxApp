package pl.gov.mf.jpk.jpkinitapp.util;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import pl.gov.mf.jpk.jpkinitapp.Main;


public class Zip4jUtil
{
    private long size;
    private File file;

    private Zip4jUtil()
    {
    }

    public Zip4jUtil(File file)
    {
        this.file = file;
    }
    
    public long getSize()
    {
        return size;
    }

    public File getFile()
    {
        return file;
    }
    

    public void createZipFile(ArrayList addFiles)
    {
        this.createZipFile(this.file, addFiles);
    }
    
    public void createZipFile(File zipFile, ArrayList addFiles)
    {
        Main.LOGGER.log(Level.FINE, "{0} method createZipFile", this.getClass().getSimpleName());

        long starttime = System.currentTimeMillis();
        
        Main.LOGGER.log(Level.FINE, "ZIP file: {0}", zipFile.getAbsolutePath());

        try
        {
            ZipFile zip = new ZipFile(zipFile);

            ZipParameters parameters = new ZipParameters();

            // COMP_STORE no compression
            // COMP_DEFLATE is for compression
            parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
            // DEFLATE_LEVEL_FASTEST = fastest compression
            // DEFLATE_LEVEL_FAST
            // DEFLATE_LEVEL_NORMAL = normal compression
            // DEFLATE_LEVEL_MAXIMUM
            // DEFLATE_LEVEL_ULTRA = maximum compression
            parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
            
            String method = "Method: ";
            switch (parameters.getCompressionMethod())
            {
                case Zip4jConstants.COMP_STORE:
                    
                    method += "STORE";
                    
                    break;
                    
                case Zip4jConstants.COMP_DEFLATE:
                    
                    method += "DEFLATE";
                    
                    break;
                    
                case Zip4jConstants.COMP_AES_ENC:
                    
                    method += "AES_ENC";
                    
                    break;
            }
            
            Main.LOGGER.log(Level.FINE, method);
            
            String level = "Level: ";
            switch (parameters.getCompressionLevel())
            {
                case Zip4jConstants.DEFLATE_LEVEL_FASTEST:
                    
                    level += "FASTEST";
                    
                    break;
                    
                case Zip4jConstants.DEFLATE_LEVEL_FAST:
                    
                    level += "FAST";
                    
                    break;
                    
                case Zip4jConstants.DEFLATE_LEVEL_NORMAL:
                    
                    level += "NORMAL";
                    
                    break;
                    
                case Zip4jConstants.DEFLATE_LEVEL_MAXIMUM:
                    
                    level += "MAXIMUM";
                    
                    break;
                    
                case Zip4jConstants.DEFLATE_LEVEL_ULTRA:
                    
                    level += "ULTRA";
                    
                    break;
            }
            
            Main.LOGGER.log(Level.FINE, level);

            zip.addFiles(addFiles, parameters);
            
            Main.printSize(this.size = zip.getFile().length());
        }
        catch (Exception ex)
        {
            Main.LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }

        Main.printTime(starttime);
    }
}

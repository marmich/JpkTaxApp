package pl.gov.mf.jpk.jpkinitapp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import pl.gov.mf.jpk.jpkinitapp.Main;


public class SplitUtil
{
    private int size;
    private File file;
    private ArrayList<File> files = new ArrayList();
    
    private SplitUtil()
    {
    }
    
    public SplitUtil(File file, int size)
    {
        this.file = file;
        this.size = size;
    }

    public ArrayList<File> getFiles()
    {
        return files;
    }
    
    public void splitFile()
    {
        Main.LOGGER.log(Level.FINE, "{0} method splitFile", this.getClass().getSimpleName());

        long starttime = System.currentTimeMillis();
        
        FileInputStream input = null;
        FileOutputStream output = null;
        
        try
        {
            input = new FileInputStream(this.file);

            int count = 0;
            int number = 0;
            int length = 0;
            
            byte[] buffer = new byte[1024*1024];
            
            while ((length = input.read(buffer)) != -1)
            {
                if (++count == 1)
                {
                    if (output != null)
                    {
                        output.close();
                    }
                    
                    this.files.add(new File(this.file.getAbsolutePath() + "." + String.format("%03d", ++number)));
                    
                    output = new FileOutputStream(this.files.get(this.files.size() - 1));
                }
                
                output.write(buffer, 0, length);
                
                if (count == this.size)
                {
                    count = 0;
                }
            }
            
            Main.LOGGER.log(Level.FINE, "Split file into {0} files.", number);
        }
        catch (IOException ex)
        {
            Main.LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        finally
        {
            if (output != null)
            {
                try
                {
                    output.close();
                }
                catch (IOException ex)
                {
                    //ignore exception
                }
            }
            
            if (input != null)
            {
                try
                {
                    input.close();
                }
                catch (IOException ex)
                {
                    //ignore exception
                }
            }
        }
        
        Main.printTime(starttime);
    }
}

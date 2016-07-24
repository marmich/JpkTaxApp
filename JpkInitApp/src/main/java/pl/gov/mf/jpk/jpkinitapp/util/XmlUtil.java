package pl.gov.mf.jpk.jpkinitapp.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.validation.Validator;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import pl.gov.mf.jpk.jpkinitapp.Main;


public class XmlUtil
{
    private File jpkFile;
    
    private String formCode = null;
    private String systemCode = null;
    private String schemaVersion = null;
    
    public static enum JpkXmlElement
    {
        Naglowek, KodFormularza, kodSystemowy, wersjaSchemy;
    }

    public static enum JpkDocumentCode
    {
        JPK_VAT, JPK_KR, JPK_WB, JPK_MAG, JPK_FA, JPK_PKPIR, JPK_EWP;
        
        public static boolean isValidCode(String value)
        {
            for (JpkDocumentCode code: JpkDocumentCode.values())
            {
                if (code.name().equals(value))
                {
                    return true;
                }
            }
            
            return false;
        }
    }
    
    private XmlUtil()
    {
    }
    
    public XmlUtil(File jpkFile)
    {
        this.jpkFile = jpkFile;
    }

    public String getFormCode()
    {
        return formCode;
    }

    public String getSystemCode()
    {
        return systemCode;
    }

    public String getSchemaVersion()
    {
        return schemaVersion;
    }
    
    public boolean isCorrectHeader()
    {
        return ((formCode != null) && (systemCode != null) &&(schemaVersion != null));
    }
    
    public boolean isSupportedCode()
    {
        return JpkDocumentCode.isValidCode(this.formCode);
    }
    
    public boolean isValidDocument()
    {
        File xsdFile;
        FileInputStream jpkStream = null;
                
        try
        {
            xsdFile = new File(Main.appLocus + File.separator + "xsd" + File.separator +"Schemat_" + this.systemCode.replace(" ", "") + "_v" + this.schemaVersion + ".xsd");
            
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(xsdFile);
            Validator validator = schema.newValidator();

            jpkStream = new FileInputStream(jpkFile);
            
            validator.validate(new SAXSource(new InputSource(jpkStream)));
        }
        catch (SAXException | IOException ex)
        {
            Main.LOGGER.log(Level.FINE, ex.getMessage(), ex);
            
            return false;
        }
        finally
        {
            if (jpkStream != null)
            {
                try
                {
                    jpkStream.close();
                }
                catch (IOException ex)
                {
                    Logger.getLogger(XmlUtil.class.getName()).log(Level.FINE, ex.getMessage(), ex);
                }
            }
        }
        
        return true;
    }
    
    public void extractJpkHeader()
    {
        Main.LOGGER.log(Level.FINE, "{0} method extractJpkHeader", this.getClass().getSimpleName());
        
        long starttime = System.currentTimeMillis();
        
        FileInputStream fis = null;
        
        try
        {
            byte[] buffer = new byte[4*1024];
            
            fis = new FileInputStream(this.jpkFile);
            
            fis.read(buffer);
            
            fis.close();
            
            String header = new String(buffer);
            
            int index;
            
            if ((index = header.indexOf("<Naglowek>")) != -1)
            {
                header = header.substring(index);
                
                if ((index = header.indexOf("</Naglowek>")) != -1)
                {
                    header = header.substring(0, index + "</Naglowek>".length());
                }
                else
                {
                    Main.LOGGER.log(Level.WARNING, "Cannot parse JPK file - unknown file structure!001");
                    
                    return;
                }
            }
            else if ((index = header.indexOf(":Naglowek>")) != -1)
            {
                String tns = header.substring(0,index);
                    
                tns = tns.substring(tns.lastIndexOf("<") + 1);
                
                header = header.substring(header.indexOf("<" + tns + ":Naglowek>"));
                
                if ((index = header.indexOf("</" + tns + ":Naglowek>")) != -1)
                {
                    header = header.substring(0, index + ("</" + tns + ":Naglowek>").length());
                }
                else
                {
                    Main.LOGGER.log(Level.WARNING, "Cannot parse JPK file - unknown file structure!");
                    
                    return;
                }
                
                header = header.replace("<" + tns + ":", "<");
                header = header.replace("</" + tns + ":", "</");
            }
            else
            {
                Main.LOGGER.log(Level.WARNING, "Cannot parse JPK file - unknown file structure!");
                
                return;
            }
            
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            
            Document document = documentFactory.newDocumentBuilder().parse(new ByteArrayInputStream(header.getBytes()));
            
            if (!JpkXmlElement.Naglowek.name().equals(document.getDocumentElement().getTagName()))
            {
                Main.LOGGER.log(Level.WARNING, "Cannot parse JPK file - unknown file structure (Element 'Naglowek' not exist)!");
                
                return;
            }
            else
            {
                NodeList naglowek = document.getDocumentElement().getChildNodes();
                
                for (int i = 0; i < naglowek.getLength(); i++)
                {
                    if (JpkXmlElement.KodFormularza.name().equals(naglowek.item(i).getNodeName()))
                    {
                        formCode = naglowek.item(i).getTextContent();
                        
                        if (naglowek.item(i).getAttributes().getNamedItem(JpkXmlElement.kodSystemowy.name()) != null)
                        {
                            systemCode = naglowek.item(i).getAttributes().getNamedItem(JpkXmlElement.kodSystemowy.name()).getNodeValue();
                        }
                        else
                        {
                            Main.LOGGER.log(Level.WARNING, "Cannot parse JPK file - unknown file structure (Attribute 'kodSystemowy' not exist)!");
        
                            return;
                        }
                        
                        if (naglowek.item(i).getAttributes().getNamedItem(JpkXmlElement.wersjaSchemy.name()) != null)
                        {
                            schemaVersion = naglowek.item(i).getAttributes().getNamedItem(JpkXmlElement.wersjaSchemy.name()).getNodeValue();
                        }
                        else
                        {
                            Main.LOGGER.log(Level.WARNING, "Cannot parse JPK file - unknown file structure (Attribute 'wersjaSchemy' not exist)!");
        
                            return;
                        }
                        
                        break;
                    }
                }
                
                if (formCode == null)
                {
                    Main.LOGGER.log(Level.WARNING, "Cannot retrieve 'formCode'!");
                }
                
                if (systemCode == null)
                {
                    Main.LOGGER.log(Level.WARNING, "Cannot retrieve 'systemCode'!");
                }
                
                if (schemaVersion == null)
                {
                    Main.LOGGER.log(Level.WARNING, "Cannot retrieve 'schemaVersion'!");
                }
            }
        }
        catch (ParserConfigurationException | SAXException | IOException ex)
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
        }
        
        Main.printTime(starttime);
    }
}

package pl.gov.mf.jpk.jpkinitapp.util;

import java.io.File;
import java.util.logging.Level;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.SAXException;
import pl.gov.mf.jpk.jpkinitapp.Main;
import pl.gov.mf.jpk.jpkinitapp.jaxb.ArrayOfDocumentType;
import pl.gov.mf.jpk.jpkinitapp.jaxb.ArrayOfDocumentType.Document;
import pl.gov.mf.jpk.jpkinitapp.jaxb.ArrayOfFileSignatureType.Encryption;
import pl.gov.mf.jpk.jpkinitapp.jaxb.ArrayOfFileSignatureType.Encryption.AES;
import pl.gov.mf.jpk.jpkinitapp.jaxb.ArrayOfFileSignatureType.Encryption.AES.IV;
import pl.gov.mf.jpk.jpkinitapp.jaxb.ArrayOfFileSignatureType.Packaging;
import pl.gov.mf.jpk.jpkinitapp.jaxb.ArrayOfFileSignatureType.Packaging.SplitZip;
import pl.gov.mf.jpk.jpkinitapp.jaxb.DocumentType;
import pl.gov.mf.jpk.jpkinitapp.jaxb.DocumentType.FileSignatureList;
import pl.gov.mf.jpk.jpkinitapp.jaxb.DocumentType.FormCode;
import pl.gov.mf.jpk.jpkinitapp.jaxb.FileSignatureType;
import pl.gov.mf.jpk.jpkinitapp.jaxb.InitUploadType;
import pl.gov.mf.jpk.jpkinitapp.jaxb.InitUploadType.EncryptionKey;


public class JaxbUtil
{
    public static final String JAXB_XMLNS = "http://e-dokumenty.mf.gov.pl";
    public static final String JAXB_NAME = "InitUpload";
    public static final String JPK_API_VERSION = "01.02.01.20160617";
    
    enum TaxDocumentType
    {
        JPK, JPKAH
    }
    
    private File jpkFile = null;
    private File jpkMeta = null;
    private XmlUtil xmlUtil = null;
    private RsaUtil rsaUtil = null;
    private AesUtil aesUtil = null;
    private DigestUtil digestJpkSha = null;
    private InitUploadType initUpload;

    private JaxbUtil()
    {
    }

    public JaxbUtil(File jpkFile, XmlUtil xmlUtil, RsaUtil rsaUtil, AesUtil aesUtil, DigestUtil digestJpkSha)
    {
        this.jpkFile = jpkFile;
        this.xmlUtil = xmlUtil;
        this.rsaUtil = rsaUtil;
        this.aesUtil = aesUtil;
        this.digestJpkSha = digestJpkSha;
        
        this.jpkMeta = new File(jpkFile.getParent() + File.separator + JAXB_NAME + Main.JPK_FILE_DOT_EXT);
    }

    public void create()
    {
        Main.LOGGER.log(Level.FINE, "{0} method create", this.getClass().getSimpleName());
        
        long starttime = System.currentTimeMillis();
        
        this.initUpload = new InitUploadType();

        initUpload.setDocumentType(TaxDocumentType.JPK.name());
        initUpload.setVersion(JPK_API_VERSION);

        EncryptionKey rsaKey = new EncryptionKey();
        
        rsaKey.setAlgorithm(rsaKey.getAlgorithm());
        rsaKey.setEncoding(rsaKey.getEncoding());
        rsaKey.setPadding(rsaKey.getPadding());
        rsaKey.setMode(rsaKey.getMode());
        
        ArrayOfDocumentType jpkList = new ArrayOfDocumentType();

        initUpload.setEncryptionKey(rsaKey);
        initUpload.setDocumentList(jpkList);

        rsaKey.setValue(this.rsaUtil.getEncryptedKeyBase64());

        Document jpkDokument = new Document();

        jpkList.setDocument(jpkDokument);
        
        FormCode formCode = new FormCode();
        
        jpkDokument.setFormCode(formCode);
        
        formCode.setValue(this.xmlUtil.getFormCode());
        
        formCode.setSystemCode(this.xmlUtil.getSystemCode());
        
        formCode.setSchemaVersion(this.xmlUtil.getSchemaVersion());

        jpkDokument.setFileName(this.jpkFile.getName());

        jpkDokument.setContentLength(this.jpkFile.length());

        DocumentType.HashValue shaHash = new DocumentType.HashValue();

        shaHash.setAlgorithm(shaHash.getAlgorithm());
        
        shaHash.setEncoding(shaHash.getEncoding());
        
        shaHash.setValue(this.digestJpkSha.getDigestBase64());

        jpkDokument.setHashValue(shaHash);
        
        FileSignatureList aesFiles = new FileSignatureList();
        
        jpkDokument.setFileSignatureList(aesFiles);
        
        Packaging aesPackaging = new Packaging();
        
        aesFiles.setPackaging(aesPackaging);
        
        SplitZip aesSplitZip = new SplitZip();
        
        aesPackaging.setSplitZip(aesSplitZip);
        
        aesSplitZip.setType(aesSplitZip.getType());
        
        aesSplitZip.setMode(aesSplitZip.getMode());
        
        Encryption aesEncryption = new Encryption();
        
        aesFiles.setEncryption(aesEncryption);
        
        AES aes = new AES();
        
        aesEncryption.setAES(aes);
        
        aes.setSize(256);
        
        aes.setBlock(16);
        
        aes.setMode(aes.getMode());
        
        aes.setPadding(aes.getPadding());
        
        IV iv = new IV();
        
        aes.setIV(iv);
        
        iv.setBytes(iv.getBytes());
        
        iv.setEncoding(iv.getEncoding());
        
        iv.setValue(aesUtil.getVecBase64());
        
        aesFiles.setFilesNumber(aesUtil.getAesFiles().size());
        
        int count = 0;
        
        for (File file: aesUtil.getAesFiles())
        {
            FileSignatureType aesFile = new FileSignatureType();
            
            aesFiles.getFileSignature().add(aesFile);
            
            aesFile.setOrdinalNumber(++count);
            aesFile.setFileName(file.getName());
            aesFile.setContentLength((int) file.length());
            
            DigestUtil md5Digest = new DigestUtil(file);
            
            md5Digest.md5File();
            
            FileSignatureType.HashValue md5Hash = new FileSignatureType.HashValue();

            md5Hash.setAlgorithm(md5Hash.getAlgorithm());

            md5Hash.setEncoding(md5Hash.getEncoding());

            md5Hash.setValue(md5Digest.getDigestBase64());
        
            aesFile.setHashValue(md5Hash);
        }
        
        Main.printTime(starttime);
    }

    public void marshal()
    {
        this.marshal(true);
    }
    
    public void marshal(boolean validate)
    {
        Main.LOGGER.log(Level.FINE, "{0} method marshal", this.getClass().getSimpleName());
        
        long starttime = System.currentTimeMillis();
        
        try
        {
            JAXBContext context = JAXBContext.newInstance(InitUploadType.class.getPackage().getName());

            Marshaller marshaller = context.createMarshaller();
            
            JAXBElement jaxbObject = new JAXBElement(new QName(JAXB_XMLNS, JAXB_NAME), InitUploadType.class, this.initUpload);

            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            if (validate)
            {
                marshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(Main.class.getResource("/pl/gov/mf/jpk/jpkinitapp/resources/initupload.xsd")));
            }
            
            marshaller.marshal(jaxbObject, this.jpkMeta);
        }
        catch (JAXBException | SAXException ex)
        {
            Main.LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            
            return;
        }
        
        Main.printTime(starttime);
        
        Main.COMPLETED = true;
    }
}

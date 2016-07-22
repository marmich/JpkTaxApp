package pl.gov.mf.jpk.jpksignapp.util;

import eu.europa.esig.dss.token.KSPrivateKeyEntry;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;


public class JKSTool
{
    public static final Logger LOGGER = Logger.getLogger(JKSTool.class.getName());

    private KeyStore keystore = null;

    public JKSTool(final URL url, final char[] key)
    {
        InputStream stream = null;

        try
        {
            keystore = KeyStore.getInstance(KeyStore.getDefaultType());

            stream = url.openStream();

            keystore.load(stream, key);
        }
        catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException ex)
        {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        finally
        {
            IOUtils.closeQuietly(stream);
        }
    }

    public X509Certificate getCertificate(String certAlias, String password)
    {
        try
        {
            Certificate cert = keystore.getCertificate(certAlias);

            if (cert == null)
            {
                return null;
            }

            if (!(cert instanceof X509Certificate))
            {
                return null;
            }

            return (X509Certificate) cert;
        }
        catch (KeyStoreException ex)
        {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }

        return null;
    }

    public KSPrivateKeyEntry getPrivateKey(String certAlias, String password)
    {
        try
        {
            final Key key = keystore.getKey(certAlias, password.toCharArray());

            if (key == null)
            {
                return null;
            }
            if (!(key instanceof PrivateKey))
            {
                return null;
            }
            final Certificate[] certificateChain = keystore.getCertificateChain(certAlias);
            KeyStore.PrivateKeyEntry privateKey = new KeyStore.PrivateKeyEntry((PrivateKey) key, certificateChain);
            KSPrivateKeyEntry ksPrivateKey = new KSPrivateKeyEntry(privateKey);
            return ksPrivateKey;
        }
        catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException ex)
        {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }

        return null;
    }
}

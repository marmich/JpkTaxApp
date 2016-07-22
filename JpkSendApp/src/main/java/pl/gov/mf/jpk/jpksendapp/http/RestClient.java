package pl.gov.mf.jpk.jpksendapp.http;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import pl.gov.mf.jpk.jpksendapp.Main;
import pl.gov.mf.jpk.jpksendapp.Main.ReleaseMode;


public class RestClient
{
    public static final Logger LOGGER = Logger.getLogger(RestClient.class.getName());

    private static final String LOGGER_LINE = "-------------------------------------------------------------------------------------------";

    private static final String JPK_URL_TST = "https://test-e-dokumenty.mf.gov.pl";
    private static final String JPK_URL_PRD = "https://e-dokumenty.mf.gov.pl";

    private static final String JPK_URL_INIT = "/api/Storage/InitUploadSigned";
    private static final String JPK_URL_FINISH = "/api/Storage/FinishUpload";
    private static final String JPK_URL_STATUS = "/api/Storage/Status/";

    private final int JPK_RM_NULL = 0;
    private final int JPK_RM_INIT = 1;
    private final int JPK_RM_UPLOAD = 2;
    private final int JPK_RM_FINISH = 3;
    private final int JPK_RM_STATUS = 4;

    private String JPK_URL_HOST = JPK_URL_TST;

    private Integer requestMode = JPK_RM_NULL;
    private Integer responseCode = null;

    private String jpkReferenceNumber = null;

    private String jpkUpo = null;
    private Integer jpkCode = null;
    private String jpkDetails = null;
    private String jpkTimestamp = null;
    private String jpkDescription = null;
    private String jpkRequestId = null;
    private String jpkMessage = null;

    private JSONArray jpkErrors = null;
    private JSONArray jpkUploads = null;
    private JSONArray jpkBlobNames = null;

    public RestClient(ReleaseMode releaseMode, Handler[] handlers)
    {
        Calendar test = GregorianCalendar.getInstance();

        test.set(2016, 6, 30);

        if ((ReleaseMode.TST == releaseMode)
            && (new Date().before(test.getTime())))
        {
            this.JPK_URL_HOST = RestClient.JPK_URL_PRD;
        }
        else
        if (ReleaseMode.PRD == releaseMode)
        {
            this.JPK_URL_HOST = RestClient.JPK_URL_PRD;
        }

        for (Handler handler : LOGGER.getHandlers())
        {
            if (Main.dailyHandler != handler)
            {
                LOGGER.removeHandler(handler);

                handler.close();
            }
        }

        for (Handler handler : handlers)
        {
            LOGGER.addHandler(handler);
        }

        LOGGER.setLevel(Level.ALL);
    }

    public boolean successInit()
    {
        return (JPK_RM_INIT == requestMode) && (jpkReferenceNumber != null) && (HttpStatus.SC_OK == responseCode);
    }

    public boolean successUpload()
    {
        return (JPK_RM_UPLOAD == requestMode) && (jpkBlobNames != null);
    }

    public boolean successFinish()
    {
        return (JPK_RM_FINISH == requestMode) && (HttpStatus.SC_OK == responseCode);
    }

    public boolean successStatus()
    {
        return (JPK_RM_STATUS == requestMode) && (HttpStatus.SC_OK == responseCode);
    }

    public boolean successUPO()
    {
        return (jpkUpo != null) && (jpkUpo.trim().length() != 0)
            && (jpkDetails != null) && (jpkDetails.trim().length() != 0);
    }

    public String getJpkReferenceNumber()
    {
        return jpkReferenceNumber;
    }

    public String getJpkDetails()
    {
        return jpkDetails;
    }

    public String getJpkUpo()
    {
        return jpkUpo;
    }

    public void initUpload(File xadesFile)
    {
        requestMode = JPK_RM_INIT;

        LOGGER.log(Level.INFO, LOGGER_LINE);

        LOGGER.log(Level.INFO, "Inicjowanie sesji:\n");

        CloseableHttpClient httpclient = null;
        CloseableHttpResponse response = null;

        try
        {
            httpclient = HttpClients.custom().setSSLSocketFactory(this.createGenerousSSLSocketFactory()).build();

            HttpPost http = new HttpPost(JPK_URL_HOST + JPK_URL_INIT);

            try
            {
                LOGGER.log(Level.FINE, "Przygotowanie danych do zainicjowania sesji:");

                EntityBuilder builder = EntityBuilder.create();

                builder.setContentType(ContentType.TEXT_XML);
                builder.setContentEncoding("UTF-8");

                builder.setFile(xadesFile);

                HttpEntity entity = builder.build();

                LOGGER.log(Level.FINE, "Request content type: {0}", ContentType.get(entity));
                LOGGER.log(Level.FINE, "Request content: {0}", xadesFile.getAbsolutePath());

                http.setEntity(entity);

                LOGGER.log(Level.FINE, "Request method: {0}", http.getMethod());
                LOGGER.log(Level.FINE, "Request path: {0}", http.getURI());

                response = httpclient.execute(http);

                responseCode = response.getStatusLine().getStatusCode();

                LOGGER.log(Level.FINE, "Response code: {0}", responseCode);

                entity = response.getEntity();

                LOGGER.log(Level.FINE, "Response content type: {0}", ContentType.get(entity));

                if (ContentType.APPLICATION_JSON.getMimeType().equals(ContentType.get(entity).getMimeType()))
                {
                    String responseContent = EntityUtils.toString(entity);

                    LOGGER.log(Level.FINE, "Response content: {0}", responseContent);

                    this.parseJsonJpkElements(new JSONObject(responseContent));

                    if (responseCode != null)
                    {
                        switch (responseCode)
                        {
                            case HttpStatus.SC_OK:
                                
                                LOGGER.log(Level.INFO, "Utworzono nową sesję o numerze referencyjnym:");
                                LOGGER.log(Level.INFO, "{0}\n", jpkReferenceNumber);
                                
                                break;
                                
                            case HttpStatus.SC_BAD_REQUEST:
                                
                                LOGGER.log(Level.WARNING, "Usługa inicjalizująca sesję zwróciła błąd:");
                                LOGGER.log(Level.WARNING, "{0}\n", jpkMessage);
                                
                                break;
                                
                            case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                                
                                LOGGER.log(Level.INFO, "Wywołanie usługi inicjalizującej sesję nie powiodło się:");
                                LOGGER.log(Level.INFO, "{0}\n", jpkMessage);
                                
                                break;
                                
                            default:
                                
                                LOGGER.log(Level.SEVERE, "Usługa inicjalizująca sesję zwróciła nieoczekiwany kod: {0}", responseCode);
                                
                                break;
                        }
                    }
                }
                else if (HttpStatus.SC_NOT_FOUND == responseCode)
                {
                    LOGGER.log(Level.WARNING, "Usługa inicjalizująca sesję jest niedostępna!");
                }
                else
                {
                    LOGGER.log(Level.SEVERE, "Usługa inicjalizująca sesję zwróciła nieoczekiwany format danych.");
                }

                EntityUtils.consume(entity);
            }
            catch (IOException | ParseException | JSONException ex)
            {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
            finally
            {
                if (response != null)
                {
                    response.close();
                }
            }
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        finally
        {
            try
            {
                if (httpclient != null)
                {
                    httpclient.close();
                }
            }
            catch (IOException ex)
            {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    public void doUpload(File xmlFile)
    {
        if (!this.successInit())
        {
            return;
        }

        requestMode = JPK_RM_UPLOAD;

        LOGGER.log(Level.INFO, "Przesyłanie plików:\n");

        CloseableHttpClient httpclient = null;
        CloseableHttpResponse response = null;

        try
        {
            httpclient = HttpClients.custom().setSSLSocketFactory(this.createGenerousSSLSocketFactory()).build();

            try
            {
                LOGGER.log(Level.FINE, "Przygotowanie danych do przesyłania plików:");

                jpkBlobNames = new JSONArray();

                for (int i = 0; i < jpkUploads.length(); i++)
                {
                    JSONObject object = jpkUploads.getJSONObject(i);

                    LOGGER.log(Level.FINE, "Request unique blob name: {0}", object.getString("BlobName"));

                    jpkBlobNames.put(object.getString("BlobName"));

                    HttpPut http = new HttpPut(object.getString("Url"));

                    JSONArray headers = object.getJSONArray("HeaderList");

                    LOGGER.log(Level.FINE, "Request header list:");

                    for (int j = 0; j < headers.length(); j++)
                    {
                        JSONObject header = headers.getJSONObject(j);

                        http.addHeader(header.getString("Key"), header.getString("Value"));

                        LOGGER.log(Level.FINE, "Request header: {0}:{1}", new Object[] {header.getString("Key"), header.getString("Value")});
                    }

                    File file = new File(xmlFile.getParent() + File.separator + object.getString("FileName"));

                    if (file.exists() && file.isFile())
                    {
                        EntityBuilder builder = EntityBuilder.create();

                        builder.setFile(file);

                        HttpEntity entity = builder.build();

                        LOGGER.log(Level.FINE, "Request content type: {0}", ContentType.get(entity));
                        LOGGER.log(Level.FINE, "Request content: {0}", file.getAbsolutePath());

                        http.setEntity(entity);

                        LOGGER.log(Level.FINE, "Request method: {0}", http.getMethod());
                        LOGGER.log(Level.FINE, "Request path: {0}", http.getURI());

                        LOGGER.log(Level.INFO, "Wysyłanie pliku {0} ...", object.getString("FileName"));

                        response = httpclient.execute(http);

                        responseCode = response.getStatusLine().getStatusCode();

                        LOGGER.log(Level.FINE, "Response code: {0}", responseCode);

                        entity = response.getEntity();

                        LOGGER.log(Level.FINE, "Response content type: {0}", ContentType.get(entity));

                        String responseContent = EntityUtils.toString(entity);

                        LOGGER.log(Level.FINE, "Response content: {0}", responseContent);

                        EntityUtils.consume(entity);

                        if (HttpStatus.SC_CREATED == responseCode)
                        {
                            LOGGER.log(Level.INFO, "... poprawnie wysłano plik {0}\n", object.getString("FileName"));
                        }
                        else
                        {
                            LOGGER.log(Level.SEVERE, "Wysłanie pliku nie powiodło się!\n");
                        }
                    }
                    else
                    {
                        LOGGER.log(Level.WARNING, "Wskazny plik {0} nie istnieje!\n", file.getName());
                    }
                }
            }
            catch (IOException | ParseException | JSONException ex)
            {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
            finally
            {
                if (response != null)
                {
                    response.close();
                }
            }
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        finally
        {
            try
            {
                if (httpclient != null)
                {
                    httpclient.close();
                }
            }
            catch (IOException ex)
            {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    public void finishUpload()
    {
        requestMode = JPK_RM_FINISH;

        LOGGER.log(Level.INFO, "Kończenie sesji:");

        CloseableHttpClient httpclient = null;
        CloseableHttpResponse response = null;

        try
        {
            httpclient = HttpClients.custom().setSSLSocketFactory(this.createGenerousSSLSocketFactory()).build();

            HttpPost http = new HttpPost(JPK_URL_HOST + JPK_URL_FINISH);

            try
            {
                LOGGER.log(Level.FINE, "Przygotowanie danych do finalizowanie sesji:");

                JSONObject object = new JSONObject();

                object.put("AzureBlobNameList", jpkBlobNames);

                object.put("ReferenceNumber", jpkReferenceNumber);

                EntityBuilder builder = EntityBuilder.create();

                builder.setContentType(ContentType.APPLICATION_JSON);

                builder.setText(object.toString());

                HttpEntity entity = builder.build();

                LOGGER.log(Level.FINE, "Request content type: {0}", ContentType.get(entity));
                LOGGER.log(Level.FINE, "Request content: {0}", object.toString());

                http.setEntity(entity);

                LOGGER.log(Level.FINE, "Request method: {0}", http.getMethod());
                LOGGER.log(Level.FINE, "Request path: {0}", http.getURI());

                response = httpclient.execute(http);

                LOGGER.log(Level.FINE, "Response content type: {0}", ContentType.get(entity));

                responseCode = response.getStatusLine().getStatusCode();

                LOGGER.log(Level.FINE, "Response code: {0}", responseCode);

                entity = response.getEntity();

                LOGGER.log(Level.FINE, "Response content type: {0}", ContentType.get(entity));

                if (ContentType.APPLICATION_JSON.getMimeType().equals(ContentType.get(entity).getMimeType()))
                {
                    String responseContent = EntityUtils.toString(entity);

                    LOGGER.log(Level.FINE, "Response content: {0}", responseContent);

                    this.parseJsonJpkElements(new JSONObject(responseContent));

                    if (HttpStatus.SC_OK == responseCode)
                    {
                        LOGGER.log(Level.INFO, "Poprawnie zakończono sesję.\n");
                    }
                    if (HttpStatus.SC_BAD_REQUEST == responseCode)
                    {
                        LOGGER.log(Level.INFO, "Usługa zakończenia sesji zwróciła błąd:");
                        LOGGER.log(Level.INFO, "{0}\n", jpkMessage);
                    }
                    else if (HttpStatus.SC_INTERNAL_SERVER_ERROR == responseCode)
                    {
                        LOGGER.log(Level.INFO, "Wywołanie usługi zakończenia sesji nie powiodło się:");
                        LOGGER.log(Level.INFO, "{0}\n", jpkMessage);
                    }
                }
                else if (HttpStatus.SC_NOT_FOUND == responseCode)
                {
                    LOGGER.log(Level.WARNING, "Usługa zakończenia sesji jest niedostępna!");
                }
                else
                {
                    LOGGER.log(Level.SEVERE, "Usługa zakończenia sesji zwróciła nieoczekiwany format danych.");
                }

                EntityUtils.consume(entity);
            }
            catch (IOException | ParseException | JSONException ex)
            {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
            finally
            {
                if (response != null)
                {
                    response.close();
                }
            }
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        finally
        {
            try
            {
                if (httpclient != null)
                {
                    httpclient.close();
                }
            }
            catch (IOException ex)
            {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    public void checkStatus()
    {
        this.checkStatus(jpkReferenceNumber);
    }

    public void checkStatus(String referenceNumber)
    {
        requestMode = JPK_RM_STATUS;

        LOGGER.log(Level.INFO, LOGGER_LINE);

        LOGGER.log(Level.INFO, "Sprawdzenie statusu sesji o numerze referencyjnym:");
        LOGGER.log(Level.INFO, "{0}\n", referenceNumber);

        CloseableHttpClient httpclient = null;
        CloseableHttpResponse response = null;

        jpkReferenceNumber = referenceNumber;

        try
        {
            httpclient = HttpClients.custom().setSSLSocketFactory(this.createGenerousSSLSocketFactory()).build();

            HttpGet http = new HttpGet(JPK_URL_HOST + JPK_URL_STATUS + referenceNumber);

            try
            {
                LOGGER.log(Level.FINE, "Przygotowanie danych do sprawdzenia statusu sesji:");

                LOGGER.log(Level.FINE, "Request method: {0}", http.getMethod());
                LOGGER.log(Level.FINE, "Request path: {0}", http.getURI());

                response = httpclient.execute(http);

                responseCode = response.getStatusLine().getStatusCode();

                LOGGER.log(Level.FINE, "Response code: {0}", responseCode);

                HttpEntity entity = response.getEntity();

                LOGGER.log(Level.FINE, "Response content type: {0}", ContentType.get(entity));

                if (ContentType.APPLICATION_JSON.getMimeType().equals(ContentType.get(entity).getMimeType()))
                {
                    String responseContent = EntityUtils.toString(entity);

                    LOGGER.log(Level.FINE, "Response content: {0}", responseContent);

                    this.parseJsonJpkElements(new JSONObject(responseContent));

                    if (responseCode != null)
                    {
                        switch (responseCode)
                        {
                            case HttpStatus.SC_OK:

                                LOGGER.log(Level.INFO, "Otrzymano odpowiedź:");
                                LOGGER.log(Level.INFO, "{0}\n", jpkDescription);

                                if ((jpkUpo != null) && (jpkUpo.trim().length() != 0))
                                {
                                    LOGGER.log(Level.INFO, "Otrzymano Urzędowe Poświadczenie Odbioru o numerze referencyjnym:");
                                    LOGGER.log(Level.INFO, "{0}\n", jpkDetails);
                                }

                                break;

                            case HttpStatus.SC_BAD_REQUEST:

                                LOGGER.log(Level.INFO, "Usługa sprawdzenia statusu zwróciła błąd:");
                                LOGGER.log(Level.INFO, "{0}\n", jpkMessage);

                                break;

                            case HttpStatus.SC_INTERNAL_SERVER_ERROR:

                                LOGGER.log(Level.INFO, "Wywołanie usługi sprawdzenia statusu nie powiodło się:");
                                LOGGER.log(Level.INFO, "{0}\n", jpkMessage);

                                break;

                            default:

                                LOGGER.log(Level.SEVERE, "Usługa sprawdzenia statusu zwróciła nieoczekiwany kod: {0}", responseCode);

                                break;
                        }
                    }
                }
                else if (HttpStatus.SC_NOT_FOUND == responseCode)
                {
                    LOGGER.log(Level.WARNING, "Usługa sprawdzenia statusu jest niedostępna!");
                }
                else
                {
                    LOGGER.log(Level.SEVERE, "Usługa sprawdzenia statusu zwróciła nieoczekiwany format danych.");
                }

                EntityUtils.consume(entity);
            }
            catch (IOException | ParseException | JSONException ex)
            {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
            finally
            {
                if (response != null)
                {
                    response.close();
                }
            }
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        finally
        {
            try
            {
                if (httpclient != null)
                {
                    httpclient.close();
                }
            }
            catch (IOException ex)
            {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    /**
     * *
     *
     * @return SSLConnectionSocketFactory that bypass certificate check and
     * bypass HostnameVerifier
     */
    private SSLConnectionSocketFactory createGenerousSSLSocketFactory()
    {
        SSLContext sslContext;

        try
        {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]
                        {
                            createGenerousTrustManager()
            }, new SecureRandom());
        }
        catch (KeyManagementException | NoSuchAlgorithmException ex)
        {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);

            return null;
        }

        return new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
    }

    private X509TrustManager createGenerousTrustManager()
    {
        return new X509TrustManager()
        {
            @Override
            public void checkClientTrusted(X509Certificate[] cert, String s) throws CertificateException
            {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] cert, String s) throws CertificateException
            {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers()
            {
                return null;
            }
        };
    }

    public void cleanup()
    {
        for (Handler handler : LOGGER.getHandlers())
        {
            if (Main.dailyHandler != handler)
            {
                LOGGER.removeHandler(handler);

                handler.close();
            }
        }
    }

    private void parseJsonJpkElements(JSONObject json)
    {
        for (JsonJpkElements jsonElement : JsonJpkElements.values())
        {
            if (json.has(jsonElement.name()) && !json.isNull(jsonElement.name()))
            {
                switch (jsonElement)
                {
                    case ReferenceNumber:

                        jpkReferenceNumber = json.getString(jsonElement.name());

                        break;

                    case Upo:

                        jpkUpo = json.getString(jsonElement.name());
                        
                        jpkUpo = jpkUpo.replace("&quot;", "\"");

                        break;

                    case Code:

                        jpkCode = json.getInt(jsonElement.name());

                        break;

                    case Details:

                        jpkDetails = json.getString(jsonElement.name());

                        break;

                    case Timestamp:

                        jpkTimestamp = json.getString(jsonElement.name());

                        break;

                    case Description:

                        jpkDescription = json.getString(jsonElement.name());

                        break;

                    case RequestId:

                        jpkRequestId = json.getString(jsonElement.name());

                        break;

                    case Message:

                        jpkMessage = json.getString(jsonElement.name());

                        break;

                    case Errors:

                        jpkErrors = json.getJSONArray(jsonElement.name());

                        break;

                    case RequestToUploadFileList:

                        jpkUploads = json.getJSONArray(jsonElement.name());

                        break;
                }
            }
        }
    }

    private enum JsonJpkElements
    {
        ReferenceNumber, Upo, Code, Details, Timestamp, Description, Message, RequestId, Errors, RequestToUploadFileList;
    }

}

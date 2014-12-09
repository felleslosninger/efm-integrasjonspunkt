package no.difi.meldingsutveksling.adresseregister.client;

import no.difi.meldingsutveksling.adresseregister.CertificateResponse;
import org.springframework.stereotype.Component;
import retrofit.RestAdapter;
import retrofit.http.GET;
import retrofit.http.Path;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Properties;

/**
 * A client for a server based adress register. The class reads a file called adresseregister.properties from
 * the class path of the code that uses it. The property file must contain a property called "adresseregister.endPointURL" pointing
 * to the ENDPoint
 *
 * @author Glenn Bech
 */

@Component
public class AdresseRegisterClient {

    private static final String ADRESSEREGISTER_ENDPOINT = "adresseregister.endPointURL";
    private static final String ADRESSEREGISTER_PROPERTIESFILE = "adresseregister.properties";
    public static final String X_509 = "X.509";
    public static final String PATH_PARAM_ORG_NR = "orgNr";

    private final String endPointURL;
    private final AdresseRegisterService adresseRegister;

    public AdresseRegisterClient() {
        Properties p;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(ADRESSEREGISTER_PROPERTIESFILE)) {
            if (is == null) {
                throw new IllegalStateException(ADRESSEREGISTER_PROPERTIESFILE + " is not on classpath");
            }
            p = new Properties();
            p.load(is);

        } catch (IOException e) {
            throw new IllegalStateException(ADRESSEREGISTER_PROPERTIESFILE + " can not be read");
        }
        endPointURL = p.getProperty(ADRESSEREGISTER_ENDPOINT);
        if (endPointURL == null) {
            throw new IllegalStateException("no property with key " + ADRESSEREGISTER_ENDPOINT +
                    " in config file " + ADRESSEREGISTER_PROPERTIESFILE);
        }
        RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint(endPointURL).setLogLevel(RestAdapter.LogLevel.FULL).build();
        adresseRegister = restAdapter.create(AdresseRegisterService.class);
    }

    /**
     * Gets the certificate for the given organisation. The returned class is a X509 certificate.
     *
     * @param orgNr organisation number (9 digits)
     * @return a The certificate for the organisaiton
     * @see java.security.cert.X509Certificate
     */
    public Certificate getCertificate(String orgNr) {
        CertificateResponse response = adresseRegister.getCertificate(orgNr);
        Certificate certificate;
        try {
            CertificateFactory f;
            f = CertificateFactory.getInstance(X_509);
            certificate = f.generateCertificate(new ByteArrayInputStream(response.getBase64EncondedCertificate().getBytes()));
        } catch (CertificateException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return certificate;
    }

    interface AdresseRegisterService {
        @GET("/adresseregister/{" + PATH_PARAM_ORG_NR + "}/crt")
        CertificateResponse getCertificate(@Path(PATH_PARAM_ORG_NR) String orgNr);
    }

    public static void main(String[] args) {
        AdresseRegisterClient client = new AdresseRegisterClient();
        client.getCertificate("958935429");
    }

}

package no.difi.meldingsutveksling.adresseregister.client;

import no.difi.meldingsutveksling.adresseregister.CertificateResponse;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.springframework.stereotype.Component;
import retrofit.ErrorHandler;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Path;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

/**
 * A client for a server based adress register. The class reads a file called adresseregister.properties from
 * the class path of the code that uses it. The property file must contain a property called "adresseregister.endPointURL" pointing
 * to the Endpoint
 *
 * @author Glenn Bech
 */

@Component
public class AdresseRegisterClient {


    public static final String X_509 = "X.509";
    public static final String PATH_PARAM_ORG_NR = "orgNr";

    private AdresseRegisterService adresseRegister;


    public AdresseRegisterClient() {
    }

    public AdresseRegisterClient(String endPointURL) {
        if (endPointURL == null) {
            throw new MeldingsUtvekslingRuntimeException();
        }
        RestAdapter restAdapter = new RestAdapter.Builder().setErrorHandler(new CertificateMissingErrorHandler()).setEndpoint(endPointURL).setLogLevel(RestAdapter.LogLevel.FULL).build();
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

    class CertificateMissingErrorHandler implements ErrorHandler {
        @Override
        public Throwable handleError(RetrofitError cause) {
            Response r = cause.getResponse();
            if (r != null && r.getStatus() == 404) {
                return new CertificateNotFoundException(cause);
            }
            return cause;
        }
    }

}

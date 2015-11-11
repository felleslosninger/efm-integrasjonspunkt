package no.difi.virksert.client;

import no.difi.certvalidator.Validator;
import no.difi.virksert.security.BusinessCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.cert.X509Certificate;

public class VirksertClient {

    private static Logger logger = LoggerFactory.getLogger(VirksertClient.class);

    private URI uri;
    private Validator validator;

    VirksertClient(String uri, String scope) {
        this(URI.create(uri), scope);
    }

    VirksertClient(URI uri, String scope) {
        this.uri = uri;

        if (scope != null)
            this.validator = BusinessCertificate.getValidator(scope);
    }

    public X509Certificate fetch(String organizationNumber) throws VirksertClientException{
        URI currentUri = uri.resolve(String.format("cert/%s.cer", organizationNumber));
        logger.debug("{} => {}", organizationNumber, currentUri);

        try {
            HttpURLConnection connection = (HttpURLConnection) currentUri.toURL().openConnection();

            switch (connection.getResponseCode()) {
                case 404:
                    throw new VirksertClientException("Certificate not found.");
                case 410:
                    throw new VirksertClientException("Certificate expired.");
                case 200:
                    X509Certificate certificate = Validator.getCertificate(new BufferedInputStream(connection.getInputStream()));

                    if (validator != null)
                        validator.validate(certificate);

                    return certificate;
                default:
                    throw new VirksertClientException(String.format("Unknown error: %s", connection.getResponseMessage()));
            }
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            throw new VirksertClientException(e.getMessage(), e);
        }
    }
}

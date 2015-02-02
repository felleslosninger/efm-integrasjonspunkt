package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.virksomhetssertifikat.*;

import java.security.cert.X509Certificate;

/**
 * Simple wrapper class around the Virksomehtssertifikatvalidator. Currently validates trust chain and
 * oid validation.
 *
 * @author Glenn Bech
 */
public class CertificateValidator {

    // todo this should not be hard coded...
    private static final String ACCEPTED_OIDS = "2.16.578.1.26.1.3.5,2.16.578.1.29.13.1.1.0,2.16.578.1.26.1.3.2,2.16.578.1.26.1.0.3.2,2.16.578.1.29.913.1.1.0,2.16.578.1.1.1.1.100";
    public static final String RESOURCE = "classpath:test-certificates.jks";
    public static final String KEYSTOREPASSWORD = "changeit";
    public static final String KEYSTORE_TYPE = "JKS";

    private DifiKeyStoreUtil util;

    public CertificateValidator() {
        util = new DifiKeyStoreUtil(RESOURCE, KEYSTOREPASSWORD, KEYSTORE_TYPE, RESOURCE, KEYSTOREPASSWORD, KEYSTORE_TYPE);
    }

    public boolean validate(X509Certificate certificate) {

        VirksomhetCertificateChainValidator chainValidator = new VirksomhetCertificateChainValidator(util, new AcceptedCertificatePolicyProvider(ACCEPTED_OIDS.split(",")));
        VirksomheCriticalOidValidator oidValidator = new VirksomheCriticalOidValidator(ACCEPTED_OIDS.split(","));
        VirksomhetExpirationDateValidator expirationDateValidator = new VirksomhetExpirationDateValidator();
        try {
            return chainValidator.isValid(certificate) && oidValidator.isValid(certificate) && expirationDateValidator.isValid(certificate);
        } catch (VirksomhetsValidationException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

}

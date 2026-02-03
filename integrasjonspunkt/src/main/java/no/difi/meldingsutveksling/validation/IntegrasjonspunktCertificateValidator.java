package no.difi.meldingsutveksling.validation;

import jakarta.annotation.PostConstruct;
import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.CertificateParserException;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryClient;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.move.common.cert.KeystoreHelper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;

@Component
@Profile("{!(test | cucumber)}")
public class IntegrasjonspunktCertificateValidator {

    private final KeystoreHelper keystoreHelper;
    private final IntegrasjonspunktProperties props;
    private final ServiceRegistryClient srClient;

    public IntegrasjonspunktCertificateValidator(KeystoreHelper keystoreHelper,
                                                 IntegrasjonspunktProperties props,
                                                 ServiceRegistryClient srClient) {
        this.keystoreHelper = keystoreHelper;
        this.props = props;
        this.srClient = srClient;
    }

    @PostConstruct
    public void validateCertificate() throws VirksertCertificateException, CertificateExpiredException {
        try {
            keystoreHelper.getX509Certificate().checkValidity();
        } catch (java.security.cert.CertificateNotYetValidException e) {
            throw new CertificateExpiredException(e.getMessage());
        }

        if (props.getFeature().isEnableDPO() || props.getFeature().isEnableDPE()) {
            String pem;
            try {
                pem = srClient.getCertificate(props.getOrg().getNumber());
            } catch (ServiceRegistryLookupException e) {
                throw new VirksertCertificateException(e);
            }

            X509Certificate cert;
            try {
                cert = CertificateParser.parse(pem);
            } catch (CertificateParserException e) {
                throw new VirksertCertificateException("Failed to parse certificate from Virksert", e);
            }
            try {
                cert.checkValidity();
            } catch (java.security.cert.CertificateNotYetValidException e) {
                throw new CertificateExpiredException(e.getMessage());
            }

            if (!keystoreHelper.getX509Certificate().getSerialNumber().equals(cert.getSerialNumber())) {
                throw new VirksertCertificateException("Keystore certificate serial number (" +
                        keystoreHelper.getX509Certificate().getSerialNumber() + ") does not match certificate in Virksert (" +
                        cert.getSerialNumber() + ")");
            }
        }
    }

}

package no.difi.meldingsutveksling.services;


import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.noarkexchange.*;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;


@Component
public class Adresseregister {

    @Autowired
    IntegrasjonspunktConfiguration configuration;

    @Autowired
    ServiceRegistryLookup serviceRegistryLookup;

    public Adresseregister() {
    }

    public Adresseregister(ServiceRegistryLookup serviceRegistryLookup) {
        this.serviceRegistryLookup = serviceRegistryLookup;
    }

    public IntegrasjonspunktConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(IntegrasjonspunktConfiguration configuration) {
        this.configuration = configuration;
    }

    public void validateCertificates(StandardBusinessDocumentWrapper documentWrapper) throws MessageException {
        try {
            getCertificate(documentWrapper.getReceiverOrgNumber());
        } catch (CertificateException e) {
            throw new MessageException(e, StatusMessage.MISSING_RECIEVER_CERTIFICATE);
        }
        try {
            getCertificate(documentWrapper.getSenderOrgNumber());
        } catch (CertificateException e) {
            throw new MessageException(e, StatusMessage.MISSING_SENDER_CERTIFICATE);
        }
    }

    public Certificate getCertificate(String orgNumber) throws CertificateException {
        String nOrgNumber = FiksFix.replaceOrgNummberWithKs(orgNumber);
        String pemCertificate = serviceRegistryLookup.getPrimaryServiceRecord(nOrgNumber).getPemCertificate();
        if (StringUtils.isEmpty(pemCertificate)) {
            throw new CertificateException("ServiceRegistry does not have public certificate for " + orgNumber);
        }
        try {
            return new CertificateParser().parse(pemCertificate);
        } catch (CertificateException | IOException e) {
            throw new CertificateException(String.format("Failed to parse pem certificate: invalid certificate for organization %s? ", orgNumber), e);
        }
    }

}

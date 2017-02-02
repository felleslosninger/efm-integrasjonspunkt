package no.difi.meldingsutveksling.services;

import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.CertificateParserException;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.StandardBusinessDocumentWrapper;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;

@Component
public class Adresseregister {

    private static final Logger log = LoggerFactory.getLogger(Adresseregister.class);

    private ServiceRegistryLookup serviceRegistryLookup;

    @Autowired
    public Adresseregister(ServiceRegistryLookup serviceRegistryLookup) {
        this.serviceRegistryLookup = serviceRegistryLookup;
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
        ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecord(orgNumber);

        String pemCertificate = serviceRecord.getPemCertificate();
        if (StringUtils.isEmpty(pemCertificate)) {
            throw new CertificateException("ServiceRegistry does not have public certificate for " + orgNumber);
        }
        try {
            return new CertificateParser().parse(pemCertificate);
        } catch (CertificateParserException e) {
            throw new CertificateException(String.format("Failed to parse pem certificate: invalid certificate for organization %s? ", orgNumber), e);
        }
    }

    public boolean hasAdresseregisterCertificate(String organisasjonsnummer) {
        log.info("hasAdresseregisterCertificate orgnr:" +organisasjonsnummer+"orgnr");
        try {
            getCertificate(organisasjonsnummer);
        } catch (CertificateException e) {
            return false;
        }
        return true;
    }

}

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

import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPE_INNSYN;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;

@Component
public class Adresseregister {

    private static final Logger log = LoggerFactory.getLogger(Adresseregister.class);

    private ServiceRegistryLookup serviceRegistryLookup;

    @Autowired
    public Adresseregister(ServiceRegistryLookup serviceRegistryLookup) {
        this.serviceRegistryLookup = serviceRegistryLookup;
    }

    public void validateCertificates(StandardBusinessDocumentWrapper documentWrapper) throws MessageException {
        ServiceRecord receiverServiceRecord = serviceRegistryLookup.getServiceRecord(documentWrapper.getReceiverOrgNumber());
        try {
            getCertificate(receiverServiceRecord);
        } catch (CertificateException e) {
            throw new MessageException(e, StatusMessage.MISSING_RECIEVER_CERTIFICATE);
        }

        ServiceRecord senderServiceRecord = serviceRegistryLookup.getServiceRecord(documentWrapper.getSenderOrgNumber());
        try {
            getCertificate(senderServiceRecord);
        } catch (CertificateException e) {
            throw new MessageException(e, StatusMessage.MISSING_SENDER_CERTIFICATE);
        }
    }

    public Certificate getCertificate(ServiceRecord serviceRecord) throws CertificateException {

        String pemCertificate = serviceRecord.getPemCertificate();
        if (StringUtils.isEmpty(pemCertificate)) {
            throw new CertificateException("ServiceRegistry does not have public certificate for " + serviceRecord.getOrganisationNumber());
        }
        try {
            return new CertificateParser().parse(pemCertificate);
        } catch (CertificateParserException e) {
            throw new CertificateException(String.format("Failed to parse pem certificate: invalid certificate for " +
                    "organization %s? ", serviceRecord.getOrganisationNumber()), e);
        }
    }

    public boolean hasAdresseregisterCertificate(ServiceRecord serviceRecord) {

        if (asList(DPV, DPE_INNSYN).contains(serviceRecord.getServiceIdentifier())) {
            return false;
        }

        log.info("hasAdresseregisterCertificate orgnr:" +serviceRecord.getOrganisationNumber());
        try {
            getCertificate(serviceRecord);
        } catch (Exception e) {
            log.warn("getCertificate: ", e);
            return false;
        }
        return true;
    }

}

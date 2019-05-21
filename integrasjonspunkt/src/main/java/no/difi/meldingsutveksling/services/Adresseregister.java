package no.difi.meldingsutveksling.services;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.CertificateParserException;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;

@Component
@RequiredArgsConstructor
public class Adresseregister {

    private final ServiceRegistryLookup serviceRegistryLookup;

    public void validateCertificates(StandardBusinessDocument sbd) throws MessageException {
        ServiceRecord receiverServiceRecord;
        try {
            receiverServiceRecord = serviceRegistryLookup.getServiceRecord(sbd.getReceiverIdentifier());
        } catch (ServiceRegistryLookupException e) {
            throw new MessageException(e, StatusMessage.MISSING_SERVICE_RECORD);
        }
        try {
            getCertificate(receiverServiceRecord);
        } catch (CertificateException e) {
            throw new MessageException(e, StatusMessage.MISSING_RECIEVER_CERTIFICATE);
        }

        ServiceRecord senderServiceRecord;
        try {
            senderServiceRecord = serviceRegistryLookup.getServiceRecord(sbd.getSenderIdentifier());
        } catch (ServiceRegistryLookupException e) {
            throw new MessageException(e, StatusMessage.MISSING_SERVICE_RECORD);
        }
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

}

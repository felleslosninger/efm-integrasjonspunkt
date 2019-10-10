package no.difi.meldingsutveksling.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.CertificateParserException;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Component
@RequiredArgsConstructor
@Slf4j
public class Adresseregister {

    private final ServiceRegistryLookup serviceRegistryLookup;

    public void validateCertificates(StandardBusinessDocument sbd) throws MessageException {
        ServiceRecord receiverServiceRecord;
        try {
            receiverServiceRecord = serviceRegistryLookup.getServiceRecord(SRParameter.builder(sbd.getReceiverIdentifier())
                    .conversationId(sbd.getConversationId()).build());
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
            senderServiceRecord = serviceRegistryLookup.getServiceRecord(SRParameter.builder(sbd.getSenderIdentifier()).build());
        } catch (ServiceRegistryLookupException e) {
            throw new MessageException(e, StatusMessage.MISSING_SERVICE_RECORD);
        }
        try {
            getCertificate(senderServiceRecord);
        } catch (CertificateException e) {
            throw new MessageException(e, StatusMessage.MISSING_SENDER_CERTIFICATE);
        }
    }

    public Certificate getReceiverCertificate(NextMoveMessage message) {
        try {
            return getCertificate(serviceRegistryLookup.getServiceRecord(SRParameter.builder(message.getReceiverIdentifier())
                            .conversationId(message.getConversationId())
                            .build(),
                    message.getSbd().getProcess(),
                    message.getSbd().getStandard()));
        } catch (ServiceRegistryLookupException e) {
            log.error(markerFrom(message), "Could not fetch service record for identifier {}", message.getReceiverIdentifier());
            throw new MeldingsUtvekslingRuntimeException(String.format("Could not fetch service record for identifier %s", message.getReceiverIdentifier()));
        } catch (CertificateException e) {
            log.error(markerFrom(message), "Could not fetch certificate for receiver {}", message.getReceiverIdentifier());
            throw new MeldingsUtvekslingRuntimeException(String.format("Could not fetch certificate for identifier %s", message.getReceiverIdentifier()));
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

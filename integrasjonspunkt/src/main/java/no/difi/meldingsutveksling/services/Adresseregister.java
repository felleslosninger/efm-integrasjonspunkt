package no.difi.meldingsutveksling.services;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.CertificateParserException;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Component
@RequiredArgsConstructor
@Slf4j
public class Adresseregister {

    private final ServiceRegistryLookup serviceRegistryLookup;

    public Certificate getReceiverCertificate(NextMoveMessage message) {
        try {
            return getCertificate(serviceRegistryLookup.getServiceRecord(SRParameter.builder(message.getReceiverIdentifier())
                            .process(message.getSbd().getProcess())
                            .conversationId(message.getConversationId())
                            .build(),
                    message.getSbd().getDocumentType()));
        } catch (ServiceRegistryLookupException e) {
            log.error(markerFrom(message), "Could not fetch service record for identifier {}", message.getReceiverIdentifier());
            throw new MeldingsUtvekslingRuntimeException("Could not fetch service record for identifier %s".formatted(message.getReceiverIdentifier()));
        } catch (CertificateException e) {
            log.error(markerFrom(message), "Could not fetch certificate for receiver {}", message.getReceiverIdentifier());
            throw new MeldingsUtvekslingRuntimeException("Could not fetch certificate for identifier %s".formatted(message.getReceiverIdentifier()));
        }
    }

    public Certificate getCertificate(ServiceRecord serviceRecord) throws CertificateException {

        String pemCertificate = serviceRecord.getPemCertificate();
        if (Strings.isNullOrEmpty(pemCertificate)) {
            throw new CertificateException("ServiceRegistry does not have public certificate for " + serviceRecord.getOrganisationNumber());
        }
        try {
            return CertificateParser.parse(pemCertificate);
        } catch (CertificateParserException e) {
            throw new CertificateException(String.format("Failed to parse pem certificate: invalid certificate for " +
                    "organization %s? ", serviceRecord.getOrganisationNumber()), e);
        }
    }

}

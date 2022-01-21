package no.difi.meldingsutveksling.services;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.CertificateParserException;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.logging.NextMoveMessageMarkers;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;

@Component
@RequiredArgsConstructor
@Slf4j
public class Adresseregister {

    private final ServiceRegistryLookup serviceRegistryLookup;
    private final NextMoveMessageMarkers nextMoveMessageMarkers;

    public Certificate getReceiverCertificate(NextMoveMessage message) {
        try {
            return getCertificate(serviceRegistryLookup.getServiceRecord(SRParameter.builder(message.getReceiverIdentifier())
                            .process(SBDUtil.getProcess(message.getSbd()))
                            .conversationId(message.getConversationId())
                            .build(),
                    SBDUtil.getDocumentType(message.getSbd())));
        } catch (ServiceRegistryLookupException e) {
            log.error(nextMoveMessageMarkers.markerFrom(message),
                    "Could not fetch service record for identifier {}", message.getReceiverIdentifier());
            throw new MeldingsUtvekslingRuntimeException(String.format("Could not fetch service record for identifier %s", message.getReceiverIdentifier()));
        } catch (CertificateException e) {
            log.error(nextMoveMessageMarkers.markerFrom(message),
                    "Could not fetch certificate for receiver {}", message.getReceiverIdentifier());
            throw new MeldingsUtvekslingRuntimeException(String.format("Could not fetch certificate for identifier %s", message.getReceiverIdentifier()));
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

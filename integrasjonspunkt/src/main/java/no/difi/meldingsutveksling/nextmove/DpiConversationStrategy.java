package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.message.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;

import java.time.Clock;

import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Component
@Slf4j
@RequiredArgsConstructor
public class DpiConversationStrategy implements ConversationStrategy {

    private final IntegrasjonspunktProperties props;
    private final ServiceRegistryLookup sr;
    private final Clock clock;
    private final OptionalCryptoMessagePersister optionalCryptoMessagePersister;
    private final MeldingsformidlerClient meldingsformidlerClient;

    @Override
    public void send(NextMoveOutMessage message) throws NextMoveException {
        ServiceRecord serviceRecord;
        try {
            serviceRecord = sr.getServiceRecord(SRParameter.builder(message.getReceiverIdentifier())
                            .conversationId(message.getConversationId())
                            .process(message.getProcessIdentifier())
                            .build(),
                    message.getSbd().getStandard());
        } catch (ServiceRegistryLookupException e) {
            throw new MeldingsUtvekslingRuntimeException(
                    String.format("Could not find service record[receiver=%s, process=%s, documentType=%s]",
                            message.getReceiverIdentifier(),
                            message.getProcessIdentifier(),
                            message.getSbd().getStandard()));
        }

        NextMoveDpiRequest request = new NextMoveDpiRequest(props, clock, message, serviceRecord, optionalCryptoMessagePersister);

        try {
            meldingsformidlerClient.sendMelding(request);
        } catch (MeldingsformidlerException e) {
            Audit.error("Failed to send message to DPI", markerFrom(message), e);
            throw new NextMoveException(e);
        }
    }
}

package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.nextmove.NextMoveMessageMarkers.markerFrom;

@Component
@Slf4j
@RequiredArgsConstructor
public class DpiConversationStrategy implements ConversationStrategy {

    private final IntegrasjonspunktProperties props;
    private final ServiceRegistryLookup sr;
    private final CryptoMessagePersister cryptoMessagePersister;
    private final MeldingsformidlerClient meldingsformidlerClient;

    @Override
    public void send(ConversationResource conversationResource) {
        throw new UnsupportedOperationException("ConversationResource no longer in use");
    }

    @Override
    public void send(NextMoveOutMessage message) throws NextMoveException {
        List<ServiceRecord> serviceRecords = sr.getServiceRecords(message.getReceiverIdentifier());
        Optional<ServiceRecord> serviceRecord = serviceRecords.stream()
                .filter(r -> message.getServiceIdentifier() == r.getServiceIdentifier())
                .findFirst();

        if (!serviceRecord.isPresent()) {
            List<ServiceIdentifier> acceptableTypes = serviceRecords.stream()
                    .map(ServiceRecord::getServiceIdentifier)
                    .collect(Collectors.toList());
            String errorStr = String.format("Message is of type '%s', but receiver '%s' accepts types '%s'.",
                    message.getServiceIdentifier(), message.getReceiverIdentifier(), acceptableTypes);
            log.error(markerFrom(message), errorStr);
            throw new NextMoveException(errorStr);
        }

        NextMoveDpiRequest request = new NextMoveDpiRequest(props, message, serviceRecord.get(), cryptoMessagePersister);

        try {
            meldingsformidlerClient.sendMelding(request);
        } catch (MeldingsformidlerException e) {
            Audit.error("Failed to send message to DPI", markerFrom(message), e);
            throw new NextMoveException(e);
        }
    }
}

package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ks.fiksio.FiksIoService;
import no.difi.meldingsutveksling.logging.Audit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "difi.move.feature.enableDPE", havingValue = "true")
public class DpeConversationStrategy implements ConversationStrategy {

    private final NextMoveServiceBus serviceBus;
    private final FiksIoService fiksIoService;

    @Override
    @Transactional
    public void send(NextMoveOutMessage message) throws NextMoveException {
//        serviceBus.putMessage(message);
        // TODO lookup receiver in fiks katalog
        fiksIoService.sendMessage(message);
        Audit.info(format("Message [id=%s, serviceIdentifier=%s] sent to service bus",
                message.getMessageId(), message.getServiceIdentifier()),
                markerFrom(message));
    }
}

package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.logging.Audit;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "difi.move.feature.enableDPE", havingValue = "true")
@Order(100)
@Slf4j
public class DpeConversationStrategyImpl implements DpeConversationStrategy {

    private final NextMoveServiceBus serviceBus;

    @Override
    @Transactional
    public void send(@NotNull NextMoveOutMessage message) throws NextMoveException {
        serviceBus.putMessage(message);
        Audit.info(format("Message [id=%s, serviceIdentifier=%s] sent to service bus",
                message.getMessageId(), message.getServiceIdentifier()),
                markerFrom(message));
    }
}

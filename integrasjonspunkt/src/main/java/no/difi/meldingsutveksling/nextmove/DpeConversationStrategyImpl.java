package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.DpeConversationStrategy;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.servicebus.NextMoveServiceBus;
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
@Order
@Slf4j
public class DpeConversationStrategyImpl implements DpeConversationStrategy {

    private final NextMoveServiceBus serviceBus;

    @Override
    @Transactional
    @Timed
    public void send(@NotNull NextMoveOutMessage message) throws NextMoveException {
        serviceBus.putMessage(message);
        Audit.info(format("Message [id=%s, serviceIdentifier=%s] sent to service bus",
                message.getMessageId(), message.getServiceIdentifier()),
                markerFrom(message));
    }
}

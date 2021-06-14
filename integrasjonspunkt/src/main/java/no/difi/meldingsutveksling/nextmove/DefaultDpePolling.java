package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.DpePolling;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.servicebus.NextMoveServiceBus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPE", havingValue = "true")
@Order
@RequiredArgsConstructor
public class DefaultDpePolling implements DpePolling {

    private final NextMoveServiceBus nextMoveServiceBus;
    private final IntegrasjonspunktProperties properties;

    private CompletableFuture<?> batchRead;

    @Override
    @Timed
    public void poll() {
        if (properties.getNextmove().getServiceBus().isBatchRead()) {
            if (this.batchRead == null || this.batchRead.isDone()) {
                log.debug("Checking for new DPE messages (batch)..");
                this.batchRead = nextMoveServiceBus.getAllMessagesBatch();
            } else {
                log.debug("DPE batch still processing..");
            }
        } else {
            log.debug("Checking for new DPE messages..");
            nextMoveServiceBus.getAllMessagesRest();
        }
    }
}

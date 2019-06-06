package no.difi.meldingsutveksling.noarkexchange.altinn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.NextMoveServiceBus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPE", havingValue = "true")
@RequiredArgsConstructor
public class DpePolling {

    private final NextMoveServiceBus nextMoveServiceBus;
    private final IntegrasjonspunktProperties properties;

    private CompletableFuture batchRead;

    public void poll() {
        if (properties.getNextmove().getServiceBus().isBatchRead()) {
            if (this.batchRead == null || this.batchRead.isDone()) {
                log.debug("Checking for new NextMove messages (batch)..");
                this.batchRead = nextMoveServiceBus.getAllMessagesBatch();
            } else {
                log.debug("Batch still processing..");
            }
        } else {
            log.debug("Checking for new NextMove messages..");
            nextMoveServiceBus.getAllMessagesRest();
        }
    }
}

package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import no.difi.meldingsutveksling.api.DpfioConversationStrategy;
import no.difi.meldingsutveksling.ks.fiksio.FiksIoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Component
@ConditionalOnProperty(name = {"difi.move.feature.enableDPFIO"}, havingValue = "true")
public class DpfIoConversationStrategyImpl implements DpfioConversationStrategy {

    private final FiksIoService fiksIoService;
    private final Logger log = LoggerFactory.getLogger(DpfIoConversationStrategyImpl.class);

    public DpfIoConversationStrategyImpl(FiksIoService fiksIoService) {
        this.fiksIoService = fiksIoService;
    }

    @Timed
    @Override
    public void send(NextMoveOutMessage message) {
        fiksIoService.sendMessage(message);
        log.info(markerFrom(message), "Message [id={}, serviceIdentifier={}] sent to FIKS IO", message.getMessageId(), message.getServiceIdentifier());
    }

}

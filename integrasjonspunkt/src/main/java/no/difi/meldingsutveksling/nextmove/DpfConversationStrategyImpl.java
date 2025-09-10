package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.DpfConversationStrategy;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.logging.Audit;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;
import static no.difi.meldingsutveksling.receipt.ReceiptStatus.*;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@Order
@Slf4j
@RequiredArgsConstructor
public class DpfConversationStrategyImpl implements DpfConversationStrategy {

    private final SvarUtService svarUtService;
    private final ConversationService conversationService;

    @Override
    @Timed
    public void send(@NotNull NextMoveOutMessage message) throws NextMoveException {

        if (SBDUtil.isReceipt(message.getSbd())) {
            log.info("Message [%s] is a receipt - not supported by DPF. Discarding message.".formatted(message.getMessageId()));
            conversationService.registerStatus(message.getMessageId(), SENDT, LEVERT, LEST);
            return;
        }

        svarUtService.send(message);

        // SvarUt garanterer leveranse etter ok mottak av melding
        conversationService.registerStatus(message.getMessageId(), SENDT, LEVERT);

        Audit.info("Message [id=%s, serviceIdentifier=%s] sent to SvarUt".formatted(
                message.getMessageId(), message.getServiceIdentifier()),
                markerFrom(message));
    }

}

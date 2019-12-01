package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.BestEduConverter;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.*;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@Order(100)
@Slf4j
@RequiredArgsConstructor
public class DpfConversationStrategyImpl implements DpfConversationStrategy {

    private final SvarUtService svarUtService;
    private final IntegrasjonspunktProperties props;
    private final NoarkClient localNoark;
    private final PutMessageRequestFactory putMessageRequestFactory;
    private final ConversationIdEntityRepo conversationIdEntityRepo;

    @Override
    public void send(@NotNull NextMoveOutMessage message) throws NextMoveException {
        svarUtService.send(message);

        Audit.info(String.format("Message [id=%s, serviceIdentifier=%s] sent to SvarUt",
                message.getMessageId(), message.getServiceIdentifier()),
                markerFrom(message));

        if (!isNullOrEmpty(props.getNoarkSystem().getType())) {
            sendAppReceipt(message);
        }
    }

    private void sendAppReceipt(NextMoveOutMessage message) {
        String conversationId = message.getConversationId();
        ConversationIdEntity convId = conversationIdEntityRepo.findByNewConversationId(message.getConversationId());
        if (convId != null) {
            log.warn("Found {} which maps to conversation {} with invalid UUID - overriding in AppReceipt.", message.getConversationId(), convId.getOldConversationId());
            conversationId = convId.getOldConversationId();
            conversationIdEntityRepo.delete(convId);
        }
        AppReceiptType appReceipt = AppReceiptFactory.from("OK", "None", "OK");
        PutMessageRequestType putMessage = putMessageRequestFactory.createAndSwitchSenderReceiver(message.getSbd(),
                BestEduConverter.appReceiptAsString(appReceipt),
                conversationId);
        localNoark.sendEduMelding(putMessage);
    }
}

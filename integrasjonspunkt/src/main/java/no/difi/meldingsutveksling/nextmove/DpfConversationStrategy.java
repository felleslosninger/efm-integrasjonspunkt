package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreFactory;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.AppReceiptFactory;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static no.difi.meldingsutveksling.nextmove.NextMoveMessageMarkers.markerFrom;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class DpfConversationStrategy implements ConversationStrategy {

    private final SvarUtService svarUtService;
    private final IntegrasjonspunktProperties props;
    private final EDUCoreFactory eduCoreFactory;
    private final NoarkClient localNoark;

    @Override
    public void send(ConversationResource conversationResource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(NextMoveOutMessage message) throws NextMoveException {
        svarUtService.send(message);

        Audit.info(String.format("Message [id=%s, serviceIdentifier=%s] sent to SvarUt",
                message.getConversationId(), message.getServiceIdentifier()),
                markerFrom(message));

        if (props.getNoarkSystem().isEnable()) {
            sendAppReceipt(message);
        }
    }

    private void sendAppReceipt(NextMoveOutMessage message) {
        AppReceiptType appReceipt = AppReceiptFactory.from("OK", "None", "OK");
        EDUCore eduCore = eduCoreFactory.create(appReceipt, message.getConversationId(),
                message.getReceiverIdentifier(), message.getSenderIdentifier());
        PutMessageRequestType putMessage = EDUCoreFactory.createPutMessageFromCore(eduCore);
        localNoark.sendEduMelding(putMessage);
    }
}

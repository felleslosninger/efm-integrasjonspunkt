package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.DphConversationStrategy;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dph.DphService;
import no.difi.meldingsutveksling.dph.client.DphClientService;
import no.difi.meldingsutveksling.dph.client.domain.SendBusinessDocumentInput;
import no.difi.meldingsutveksling.dph.client.internal.DphParcelService;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.move.common.dokumentpakking.domain.Document;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.UUID;

import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Slf4j
@RequiredArgsConstructor
public class DphConversationStrategyImpl implements DphConversationStrategy {

    private final DphService dphService;
    private final DphClientService dphClientService;
    private final DphParcelService dphParcelService;
    private final ConversationService conversationService;
    private final NextMoveMessageService nextMoveMessageService;

    @Override
    @Timed
    public void send(NextMoveOutMessage message) throws NextMoveException {

        try {
            sendInternal(message);
        } catch (Exception e) {
            Audit.error("Error sending message with messageId=%s to Norsk Helsenett".formatted(message.getMessageId()), markerFrom(message), e);
            throw e;
        }

        Audit.info("Message [id=%s, serviceIdentifier=%s] sent to Norsk Helsenett".formatted(
                message.getMessageId(), message.getServiceIdentifier()),
            markerFrom(message));
    }

    private void sendInternal(NextMoveOutMessage message) {
        NhnIdentifier sender = message.getSender().as(NhnIdentifier.class).orElseThrow(() -> new IllegalArgumentException("Missing sender!"));
        NhnIdentifier receiver = message.getReceiver().as(NhnIdentifier.class).orElseThrow(() -> new IllegalArgumentException("Missing receiver!"));

        StandardBusinessDocument sbd = message.getSbd();

        if (receiver.getType() == NhnIdentifier.Type.FASTLEGE_FOR) {
            InfoRecord infoRecordReceiver = dphService.getInfoRecord(receiver);
            sbd.setReceiverIdentifier(NhnIdentifier.herId(infoRecordReceiver.getHerId()));
        }

        Iso6523 onBehalfOf = dphService.getOnBehalfOf(sender);

        List<Document> documents = nextMoveMessageService.getDocuments(message);
        Resource encryptedAsic = documents.isEmpty() ? null : dphParcelService.createAndEncryptAsic(documents.stream());

        SendBusinessDocumentInput input = new SendBusinessDocumentInput()
            .setSbd(sbd)
            .setEncryptedAsic(encryptedAsic);

        log.debug("DPH Sending {} messageId={}, conversationId={}", sbd.getType(), message.getMessageId(), message.getConversationId());
        UUID nhnMessageId = dphClientService.sendBusinessDocument(onBehalfOf, input);
        log.info("DPH message sent: messageId={}, externalSystemReference={}", message.getMessageId(), nhnMessageId);

        storeExternalSystemReference(message, nhnMessageId);
    }

    private void storeExternalSystemReference(NextMoveOutMessage message, UUID nhnMessageId) {
        conversationService.findConversation(message.getMessageId()).ifPresent(c -> {
            c.setExternalSystemReference(nhnMessageId.toString());
            conversationService.save(c);
        });
    }
}

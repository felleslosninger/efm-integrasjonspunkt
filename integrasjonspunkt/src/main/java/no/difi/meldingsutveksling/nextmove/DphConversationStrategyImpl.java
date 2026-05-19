package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.DphConversationStrategy;
import no.difi.meldingsutveksling.domain.BusinessMessage;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.dph.DphService;
import no.difi.meldingsutveksling.dph.client.DphClient;
import no.difi.meldingsutveksling.dph.client.domain.SendApplicationReceiptInput;
import no.difi.meldingsutveksling.dph.client.domain.SendBusinessDocumentInput;
import no.difi.meldingsutveksling.dph.client.internal.DphParcelService;
import no.difi.meldingsutveksling.exceptions.ConversationMissingExternalSystemReferenceException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.move.common.dokumentpakking.domain.Document;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Slf4j
@RequiredArgsConstructor
public class DphConversationStrategyImpl implements DphConversationStrategy {

    private final DphClient dphClient;
    private final DphService dphService;
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

        InfoRecord infoRecordReceiver = dphService.getInfoRecord(receiver);

        Iso6523 onBehalfOf = dphService.getOnBehalfOf(sender);

        List<Document> documents = nextMoveMessageService.getDocuments(message);
        Resource encryptedAsic = dphParcelService.createAndEncryptAsic(documents.stream());

        BusinessMessage businessMessage = message.getBusinessMessage();

        switch (businessMessage) {
            case DialogmeldingMessage dialogmelding -> {
                SendBusinessDocumentInput input = new SendBusinessDocumentInput()
                    .setSenderHerId(sender.getHerId())
                    .setReceiverHerId(infoRecordReceiver.getHerId())
                    .setConversationId(conversationService.getExternalSystemReference(message.getConversationId()).orElse(null))
                    .setParentId(Optional.ofNullable(message.getSbd().getParentId())
                        .flatMap(conversationService::getExternalSystemReference)
                        .orElse(null))
                    .setMessageId(message.getMessageId())
                    .setPayload(dialogmelding)
                    .setEncryptedAsic(encryptedAsic);
                log.debug("DPH Sending dialogmelding messageId={}, conversationId={}, input = {}", message.getMessageId(), message.getConversationId(), input);

                UUID nhnMessageId = dphClient.sendBusinessDocument(onBehalfOf, input);

                log.info("DPH message sent: messageId={}, externalSystemReference={}", message.getMessageId(), nhnMessageId);

                conversationService.findConversation(message.getMessageId()).ifPresent(c -> {
                    c.setExternalSystemReference(nhnMessageId.toString());
                    conversationService.save(c);
                });
            }
            case DialogmeldingKvitteringMessage kvittering -> {
                kvittering.setRelatedToMessageId(conversationService.getExternalSystemReference(kvittering.getRelatedToMessageId())
                    .orElseThrow(() -> new ConversationMissingExternalSystemReferenceException(kvittering.getRelatedToMessageId()))
                );

                UUID nhnMessageId = dphClient.sendApplicationReceipt(onBehalfOf, new SendApplicationReceiptInput()
                    .setSenderHerId(sender.getHerId())
                    .setPayload(kvittering)
                );

                conversationService.findConversation(message.getMessageId()).ifPresent(c -> {
                    c.setExternalSystemReference(nhnMessageId.toString());
                    conversationService.save(c);
                });
            }
            default ->
                throw new IllegalArgumentException("Unknown message type: " + businessMessage.getClass().getName());
        }
    }
}

package no.difi.meldingsutveksling.noarkexchange;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.bestedu.PutMessageRequestFactory;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.BestEduConverter;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingKvitteringMessage;
import no.difi.meldingsutveksling.nextmove.KvitteringStatusMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Service
@Slf4j
public class BestEduAppReceiptService {

    private final IntegrasjonspunktProperties properties;
    private final SBDFactory createSBD;
    private final NextMoveMessageService nextMoveMessageService;
    private final PutMessageRequestFactory putMessageRequestFactory;
    private final NoarkClient noarkClient;
    private final UUIDGenerator uuidGenerator;
    private final ConversationIdEntityRepo conversationIdEntityRepo;

    public BestEduAppReceiptService(
            IntegrasjonspunktProperties properties,
            SBDFactory createSBD,
            @Lazy NextMoveMessageService nextMoveMessageService,
            PutMessageRequestFactory putMessageRequestFactory,
            @Qualifier("localNoark") ObjectProvider<NoarkClient> localNoark,
            UUIDGenerator uuidGenerator,
            ConversationIdEntityRepo conversationIdEntityRepo) {
        this.properties = properties;
        this.createSBD = createSBD;
        this.nextMoveMessageService = nextMoveMessageService;
        this.putMessageRequestFactory = putMessageRequestFactory;
        this.noarkClient = localNoark.getIfAvailable();
        this.uuidGenerator = uuidGenerator;
        this.conversationIdEntityRepo = conversationIdEntityRepo;
    }

    public void sendBestEduErrorAppReceipt(NextMoveOutMessage message, String errorText) {
        AppReceiptType appReceipt = AppReceiptFactory.from("ERROR", "Unknown", errorText);
        PutMessageRequestType putMessage = putMessageRequestFactory.create(message.getSbd(), BestEduConverter.appReceiptAsString(appReceipt));
        noarkClient.sendEduMelding(putMessage);
    }

    public void sendBestEduErrorAppReceipt(StandardBusinessDocument sbd) {
        String errorText = String.format("Feilet under mottak hos %s - ble ikke avlevert sakarkivsystem", sbd.getReceiverIdentifier());
        ArkivmeldingKvitteringMessage kvittering = new ArkivmeldingKvitteringMessage()
                .setReceiptType("ERROR")
                .addMessage(new KvitteringStatusMessage("Unknown", errorText));

        StandardBusinessDocument receiptSbd = createSBD.createNextMoveSBD(
                sbd.getReceiverIdentifier(),
                sbd.getSenderIdentifier(),
                SBDUtil.getConversationId(sbd),
                uuidGenerator.generate(),
                properties.getArkivmelding().getReceiptProcess(),
                properties.getArkivmelding().getReceiptDocumentType(),
                kvittering);

        NextMoveOutMessage message = nextMoveMessageService.createMessage(receiptSbd);
        nextMoveMessageService.sendMessage(message);
    }

    /**
     * Used to send AppReceipt to sender for other cases than DPO.
     *
     * @param message nextmove message
     */
    public void sendAppReceiptToLocalNoark(NextMoveOutMessage message) {
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
        try {
            noarkClient.sendEduMelding(putMessage);
        } catch (Exception e) {
            log.error(markerFrom(message), "Error sending AppReceipt to localNoark", e);
        }
    }
}

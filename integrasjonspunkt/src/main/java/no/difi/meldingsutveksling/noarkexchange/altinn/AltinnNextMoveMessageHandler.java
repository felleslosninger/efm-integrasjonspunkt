package no.difi.meldingsutveksling.noarkexchange.altinn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.kvittering.SBDReceiptFactory;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveQueue;
import no.difi.meldingsutveksling.nextmove.StatusMessage;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.receipt.MessageStatusFactory;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@RequiredArgsConstructor
public class AltinnNextMoveMessageHandler implements AltinnMessageHandler {

    private final IntegrasjonspunktProperties properties;
    private final InternalQueue internalQueue;
    private final ConversationService conversationService;
    private final NextMoveQueue nextMoveQueue;
    private final SBDReceiptFactory sbdReceiptFactory;
    private final MessageStatusFactory messageStatusFactory;
    private final SBDUtil sbdUtil;

    @Override
    public void handleStandardBusinessDocument(StandardBusinessDocument sbd) {
        log.debug(format("NextMove message id=%s", sbd.getConversationId()));

        if (sbdUtil.isStatus(sbd)) {
            handleStatus(sbd);
        } else {
            if (properties.getNoarkSystem().isEnable() && !properties.getNoarkSystem().getEndpointURL().isEmpty()) {
                internalQueue.enqueueNoark(sbd);
            } else {
                nextMoveQueue.enqueue(sbd, DPO);
            }
            conversationService.registerStatus(sbd.getConversationId(), messageStatusFactory.getMessageStatus(ReceiptStatus.INNKOMMENDE_MOTTATT));
            sendReceivedStatusToSender(sbd);
        }
    }

    private void handleStatus(StandardBusinessDocument sbd) {
        StatusMessage status = (StatusMessage) sbd.getAny();
        MessageStatus ms = messageStatusFactory.getMessageStatus(status.getStatus());
        conversationService.registerStatus(sbd.getConversationId(), ms);
    }

    private void sendReceivedStatusToSender(StandardBusinessDocument sbd) {
        StandardBusinessDocument statusSbd = sbdReceiptFactory.createArkivmeldingStatusFrom(sbd, DocumentType.STATUS, ReceiptStatus.MOTTATT);
        NextMoveOutMessage msg = NextMoveOutMessage.of(statusSbd, DPO);
        internalQueue.enqueueNextMove(msg);
    }
}

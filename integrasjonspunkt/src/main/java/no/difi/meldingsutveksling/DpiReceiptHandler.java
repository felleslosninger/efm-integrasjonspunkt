package no.difi.meldingsutveksling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.ExternalReceipt;
import no.difi.meldingsutveksling.status.MessageStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static no.difi.meldingsutveksling.receipt.ReceiptStatus.LEVERT;
import static no.difi.meldingsutveksling.receipt.ReceiptStatus.MOTTATT;

@RequiredArgsConstructor
@Slf4j
public class DpiReceiptHandler {

    private final ConversationService conversationService;
    private final IntegrasjonspunktProperties properties;

    @Transactional
    public void handleReceipt(ExternalReceipt externalReceipt) {
        externalReceipt.confirmReceipt();

        final String conversationId = externalReceipt.getConversationId();
        Optional<Conversation> conversation = conversationService.findConversation(conversationId, ConversationDirection.OUTGOING);

        if (conversation.isPresent()) {
            MessageStatus status = externalReceipt.toMessageStatus();

            // If LEVERT comes before MOTTATT
            if (ReceiptStatus.valueOf(status.getStatus()) == LEVERT && conversation.get().getMessageStatuses().stream().noneMatch(ms -> ms.getStatus().equals("MOTTATT"))) {
                // Ensures that status MOTTATT is registered before LEVERT
                // If MOTTATT was previously registered this attempt will be discarded and not cause duplicates
                MessageStatus mottatt = MessageStatus.of(MOTTATT, status.getLastUpdate().minusSeconds(1));
                conversationService.registerStatus(conversation.get(), mottatt);
            }

            // If MOTTATT comes after LEVERT.
            if (ReceiptStatus.valueOf(status.getStatus()) == MOTTATT) {
                if (conversation.get().getMessageStatuses().stream().noneMatch(ms -> ms.getStatus().equals("MOTTATT"))) {
                    conversationService.registerStatus(conversation.get(), status);
                    log.debug(externalReceipt.logMarkers(), "Updated receipt (DPI)");
                }
            } else {
                conversationService.registerStatus(conversation.get(), status);
                log.debug(externalReceipt.logMarkers(), "Updated receipt (DPI)");
            }


        } else {
            log.warn("Unknown conversationID = {}", conversationId);
        }

        log.debug(externalReceipt.logMarkers(), "Confirmed receipt (DPI)");
    }

}

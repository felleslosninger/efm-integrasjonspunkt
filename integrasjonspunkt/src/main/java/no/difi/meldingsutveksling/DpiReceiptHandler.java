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
            boolean isProxyClient = "xmlsoap".equals(properties.getDpi().getReceiptType());
            if (ReceiptStatus.valueOf(status.getStatus()) == LEVERT && !isProxyClient) {
                // Ensures that status MOTTATT is registered before LEVERT
                // If MOTTATT was previously registered this attempt will be discarded and not cause duplicates
                // If integrasjonspunktet is configured for use from the DPI proxy client MOTTATT won't be registered at
                // all, because it causes the proxy client to crash (!)
                MessageStatus mottatt = MessageStatus.of(MOTTATT, status.getLastUpdate().minusSeconds(1));
                conversationService.registerStatus(conversation.get(), mottatt);
            }

            conversationService.registerStatus(conversation.get(), status);
            log.debug(externalReceipt.logMarkers(), "Updated receipt (DPI)");
        } else {
            log.warn("Unknown conversationID = {}", conversationId);
        }

        log.debug(externalReceipt.logMarkers(), "Confirmed receipt (DPI)");
    }

}

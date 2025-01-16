package no.difi.meldingsutveksling.api;

import no.difi.meldingsutveksling.MessageInformable;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;

import java.util.Optional;

public interface ConversationService {

    Optional<Conversation> registerStatus(String messageId, MessageStatus status);
    Optional<Conversation> registerStatus(String messageId, ReceiptStatus... status);
    Optional<Conversation> registerStatus(String messageId, ReceiptStatus status, String description);
    Optional<Conversation> registerStatus(String messageId, ReceiptStatus status, String description, String rawReceipt);
    Conversation registerStatus(Conversation conversation, MessageStatus status);
    Conversation save(Conversation conversation);
    Conversation registerConversation(MessageInformable message, ReceiptStatus... statuses);
    Conversation registerConversation(StandardBusinessDocument sbd, ServiceIdentifier si, ConversationDirection direction, ReceiptStatus... statuses);
    Optional<Conversation> findConversation(String messageId);
    Optional<Conversation> findConversation(String conversationId, ConversationDirection direction);

}
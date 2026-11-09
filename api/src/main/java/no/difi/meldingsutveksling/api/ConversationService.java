package no.difi.meldingsutveksling.api;

import no.difi.meldingsutveksling.MessageInformable;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;

import java.util.Collection;
import java.util.Optional;

public interface ConversationService {

    Optional<Conversation> registerStatus(String messageId, MessageStatus status);

    Optional<Conversation> registerStatus(String messageId, ReceiptStatus... status);

    Optional<Conversation> registerStatus(String messageId, ReceiptStatus status, String description);

    Optional<Conversation> registerStatus(String messageId, ReceiptStatus status, String description, String rawReceipt);

    Conversation registerStatus(Conversation conversation, MessageStatus status);

    /**
     * Registers the statuses on a freshly loaded and row-locked copy of the conversation, so that the duplicate check
     * sees statuses committed by concurrent pollers or other instances since {@code conversation} was loaded.
     */
    Conversation registerStatusesWithLock(Conversation conversation, Collection<MessageStatus> statuses);

    Conversation save(Conversation conversation);

    Conversation registerConversation(MessageInformable message, ReceiptStatus... statuses);

    Conversation registerConversation(StandardBusinessDocument sbd, ServiceIdentifier si, ConversationDirection direction, ReceiptStatus... statuses);

    Optional<Conversation> findConversation(String messageId);

    Optional<Conversation> findConversation(String conversationId, ConversationDirection direction);

    /**
     * Like {@link #findConversation(String, ConversationDirection)}, but row-locks the conversation until the
     * surrounding transaction ends. Must be called inside a transaction, before the conversation is loaded in it.
     */
    Optional<Conversation> findConversationForUpdate(String conversationId, ConversationDirection direction);
}

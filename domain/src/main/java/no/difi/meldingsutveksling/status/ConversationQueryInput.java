package no.difi.meldingsutveksling.status;

import lombok.Data;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class ConversationQueryInput {

    String conversationId;
    String messageId;
    String receiver;
    String receiverIdentifier;
    String sender;
    String senderIdentifier;
    String serviceIdentifier;
    String messageReference;
    String messageTitle;
    Boolean pollable;
    Boolean finished;
    ConversationDirection direction;
    /**
     * Lower bound (inclusive) for the last update date of the conversation.
     * <p>
     * Expected format: {@code yyyy-MM-dd}.
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDate lastUpdateFrom;
    /**
     * Upper bound (inclusive) for the last update date of the conversation.
     * <p>
     * Expected format: {@code yyyy-MM-dd}.
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDate lastUpdateTo;

}

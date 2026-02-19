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
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDate lastUpdateFrom;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDate lastUpdateTo;

}

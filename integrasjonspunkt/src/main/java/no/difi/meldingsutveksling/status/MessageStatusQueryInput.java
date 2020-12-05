package no.difi.meldingsutveksling.status;

import lombok.Data;

@Data
public class MessageStatusQueryInput {

    Long id;
    String conversationId;
    String messageId;
    String status;

}


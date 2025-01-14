package no.difi.meldingsutveksling.status;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.OffsetDateTime;

@Data
public class MessageStatusQueryInput {

    Long id;
    String conversationId;
    String messageId;
    String status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    OffsetDateTime fromDateTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    OffsetDateTime toDateTime;
}


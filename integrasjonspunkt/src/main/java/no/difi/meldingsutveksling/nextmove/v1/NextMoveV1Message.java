package no.difi.meldingsutveksling.nextmove.v1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NextMoveV1Message {

    String conversationId;
    String serviceIdentifier;
    String senderId;
    String receiverId;
    String senderName;
    String receiverName;
    LocalDateTime lastUpdate;
    Map<String, String> customProperties;
    Map<Integer, String> fileRefs;

}

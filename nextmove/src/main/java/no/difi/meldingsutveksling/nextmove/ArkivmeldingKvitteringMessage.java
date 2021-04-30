package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@NextMoveBusinessMessage("arkivmelding_kvittering")
public class ArkivmeldingKvitteringMessage extends BusinessMessage<ArkivmeldingKvitteringMessage> {

    private String receiptType;
    private String relatedToMessageId;
    private Set<KvitteringStatusMessage> messages;

    @JsonIgnore
    public ArkivmeldingKvitteringMessage addMessage(KvitteringStatusMessage message) {
        if (this.messages == null) {
            this.messages = new HashSet<>();
        }
        this.messages.add(message);
        return this;
    }
}

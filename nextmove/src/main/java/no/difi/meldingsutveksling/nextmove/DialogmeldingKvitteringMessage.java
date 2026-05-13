package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString
@NoArgsConstructor
@XmlRootElement(name = "dialogmelding_kvittering", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class DialogmeldingKvitteringMessage extends BusinessMessageAsAttachment<DialogmeldingKvitteringMessage> {

    private String relatedToMessageId;
    private DialogmeldingKvitteringStatus status;
    private Set<KvitteringStatusMessage> messages;

    @JsonIgnore
    public DialogmeldingKvitteringMessage addMessage(KvitteringStatusMessage message) {
        if (this.messages == null) {
            this.messages = new HashSet<>();
        }
        this.messages.add(message);
        return this;
    }
}

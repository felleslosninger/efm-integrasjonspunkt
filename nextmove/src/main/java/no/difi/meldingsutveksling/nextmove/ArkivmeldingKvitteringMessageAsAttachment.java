package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "arkivmelding_kvittering", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class ArkivmeldingKvitteringMessageAsAttachment extends BusinessMessageAsAttachment<ArkivmeldingKvitteringMessageAsAttachment> {

    private String receiptType;
    private String relatedToMessageId;
    private Set<KvitteringStatusMessage> messages;

    @JsonIgnore
    public ArkivmeldingKvitteringMessageAsAttachment addMessage(KvitteringStatusMessage message) {
        if (this.messages == null) {
            this.messages = new HashSet<>();
        }
        this.messages.add(message);
        return this;
    }
}

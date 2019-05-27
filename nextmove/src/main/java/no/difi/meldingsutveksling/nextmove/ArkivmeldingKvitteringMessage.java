package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Sets;
import lombok.*;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Set;

@Getter
@Setter
@ToString
@Entity
@DiscriminatorValue("arkivmelding_kvittering")
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "arkivmelding_kvittering", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class ArkivmeldingKvitteringMessage extends BusinessMessage {

    private String receiptType;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<KvitteringStatusMessage> messages;

    @JsonIgnore
    public ArkivmeldingKvitteringMessage addMessage(KvitteringStatusMessage message) {
        if (this.messages == null) {
            this.messages = Sets.newHashSet();
        }
        this.messages.add(message);
        return this;
    }
}

package no.difi.meldingsutveksling.nextmove;

import lombok.Data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DPF")
@Data
public class DpfConversationResource extends ConversationResource {

    DpfConversationResource() {}

}

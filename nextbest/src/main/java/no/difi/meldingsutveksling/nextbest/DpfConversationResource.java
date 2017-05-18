package no.difi.meldingsutveksling.nextbest;

import lombok.Data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DPF")
@Data
public class DpfConversationResource extends ConversationResource {

    DpfConversationResource() {}

}

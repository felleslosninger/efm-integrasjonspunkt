package no.difi.meldingsutveksling.nextmove;

import lombok.Data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DPE_data")
@Data
public class DpeDataConversationResource extends ConversationResource {

    DpeDataConversationResource() {}
}

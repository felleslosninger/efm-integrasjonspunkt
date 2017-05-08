package no.difi.meldingsutveksling.nextbest;

import lombok.Data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DPE_data")
@Data
public class DpeDataConversationResource extends ConversationResource {

    DpeDataConversationResource() {}
}

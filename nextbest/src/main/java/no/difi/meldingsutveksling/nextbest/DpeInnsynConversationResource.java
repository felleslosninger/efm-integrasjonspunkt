package no.difi.meldingsutveksling.nextbest;

import lombok.Data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DPE_innsyn")
@Data
public class DpeInnsynConversationResource extends ConversationResource {

    DpeInnsynConversationResource() {}
}

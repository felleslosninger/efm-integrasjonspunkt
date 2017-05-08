package no.difi.meldingsutveksling.nextbest;

import lombok.Data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DPO")
@Data
public class DpoConversationResource extends ConversationResource {

    private String jpId;

    DpoConversationResource() {}

}

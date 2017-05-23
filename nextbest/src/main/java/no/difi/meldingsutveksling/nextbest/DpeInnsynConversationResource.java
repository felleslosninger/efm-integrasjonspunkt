package no.difi.meldingsutveksling.nextbest;

import lombok.Data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@DiscriminatorValue("DPE_innsyn")
@Data
@XmlRootElement
public class DpeInnsynConversationResource extends ConversationResource {

    DpeInnsynConversationResource() {}
}

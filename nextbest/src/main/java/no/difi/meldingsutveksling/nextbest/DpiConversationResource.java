package no.difi.meldingsutveksling.nextbest;

import lombok.Data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DPI")
@Data
public class DpiConversationResource extends ConversationResource {

    DpiConversationResource() {}

}

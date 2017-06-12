package no.difi.meldingsutveksling.nextmove;

import lombok.Data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DPI")
@Data
public class DpiConversationResource extends ConversationResource {

    DpiConversationResource() {}

}

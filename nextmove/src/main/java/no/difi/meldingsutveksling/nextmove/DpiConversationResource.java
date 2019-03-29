package no.difi.meldingsutveksling.nextmove;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DPI_DIGITAL")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DpiConversationResource extends ConversationResource {

    private String title;

}

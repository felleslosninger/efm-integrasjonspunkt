package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.difi.meldingsutveksling.nextmove.dpi.DigitalPostInfo;
import no.difi.meldingsutveksling.nextmove.dpi.FysiskPostInfo;

import javax.persistence.*;
import java.util.List;

@Entity
@DiscriminatorValue("DPI")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DpiConversationResource extends ConversationResource {

    @Embedded
    private DigitalPostInfo digitalPostInfo;
    @Embedded
    private FysiskPostInfo fysiskPostInfo;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "conversation_id")
    private List<FileAttachement> files;
}

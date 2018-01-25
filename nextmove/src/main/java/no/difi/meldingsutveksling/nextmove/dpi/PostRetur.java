package no.difi.meldingsutveksling.nextmove.dpi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.difi.sdp.client2.domain.fysisk_post.Returhaandtering;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class PostRetur {

    @Embedded
    private ReturMottaker mottaker;
    private Returhaandtering postHaandtering;
}

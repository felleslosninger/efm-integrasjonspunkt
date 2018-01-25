package no.difi.meldingsutveksling.nextmove.dpi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.difi.sdp.client2.domain.fysisk_post.Posttype;
import no.difi.sdp.client2.domain.fysisk_post.Utskriftsfarge;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class FysiskPostInfo {

    private Utskriftsfarge utskriftsfarge;
    private Posttype posttype;
    @Embedded
    private PostRetur retur;
}

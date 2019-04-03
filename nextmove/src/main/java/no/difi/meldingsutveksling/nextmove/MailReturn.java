package no.difi.meldingsutveksling.nextmove;

import lombok.*;
import no.difi.sdp.client2.domain.fysisk_post.Returhaandtering;

import javax.persistence.Embeddable;
import javax.persistence.Entity;

@Getter
@Setter
@ToString
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class MailReturn {

    private PostAddress receiver;
    private Returhaandtering returnHandling;
}

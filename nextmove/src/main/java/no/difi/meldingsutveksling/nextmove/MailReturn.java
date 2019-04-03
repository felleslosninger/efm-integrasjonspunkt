package no.difi.meldingsutveksling.nextmove;

import lombok.*;
import no.difi.sdp.client2.domain.fysisk_post.Returhaandtering;

import javax.persistence.*;

@Getter
@Setter
@ToString
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "mail_return")
public class MailReturn extends AbstractEntity<Long> {

    @Embedded
    private PostAddress receiver;
    private Returhaandtering returnHandling;
}

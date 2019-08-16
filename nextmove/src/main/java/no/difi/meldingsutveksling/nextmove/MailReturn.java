package no.difi.meldingsutveksling.nextmove;

import lombok.*;
import no.difi.sdp.client2.domain.fysisk_post.Returhaandtering;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "mail_return")
public class MailReturn extends AbstractEntity<Long> {

    @Embedded
    @NotNull
    @Valid
    private PostAddress mottaker;

    @NotNull
    private Returhaandtering returhaandtering;
}

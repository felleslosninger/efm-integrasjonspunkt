package no.difi.meldingsutveksling.dpo;

import lombok.*;
import no.difi.meldingsutveksling.nextmove.AbstractEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Entity
public class MessageChannelEntry extends AbstractEntity<Long> {

    @Column(unique = true)
    @NonNull
    private String messageId;
    @NonNull
    private String channel;
}

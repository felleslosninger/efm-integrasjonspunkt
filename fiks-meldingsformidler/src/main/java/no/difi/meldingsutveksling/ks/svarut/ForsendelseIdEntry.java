package no.difi.meldingsutveksling.ks.svarut;

import lombok.*;
import no.difi.meldingsutveksling.nextmove.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Entity
public class ForsendelseIdEntry extends AbstractEntity<Long> {

    @Column(unique = true)
    @NonNull
    private String messageId;
    @NonNull
    private String forsendelseId;
}

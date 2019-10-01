package no.difi.meldingsutveksling.noarkexchange;

import lombok.*;
import no.difi.meldingsutveksling.nextmove.AbstractEntity;

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
public class ConversationIdEntity extends AbstractEntity<Long> {

    @NonNull
    private String oldConversationId;
    @NonNull
    private String newConversationId;

}

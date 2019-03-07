package no.difi.meldingsutveksling.nextmove;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("in")
public class NextMoveInMessage extends NextMoveMessage {
}

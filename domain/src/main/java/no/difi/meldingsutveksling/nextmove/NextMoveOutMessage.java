package no.difi.meldingsutveksling.nextmove;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("out")
public class NextMoveOutMessage extends NextMoveMessage {
}

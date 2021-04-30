package no.difi.meldingsutveksling.nextmove;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@NextMoveBusinessMessage("avtalt")
public class AvtaltMessage extends BusinessMessage<AvtaltMessage> {
    String identifier;
    Object content;
}
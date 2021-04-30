package no.difi.meldingsutveksling.nextmove;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@NextMoveBusinessMessage("fiksio")
public class FiksIoMessage extends BusinessMessage<FiksIoMessage> {
}

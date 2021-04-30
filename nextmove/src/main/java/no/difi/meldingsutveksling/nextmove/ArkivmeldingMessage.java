package no.difi.meldingsutveksling.nextmove;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@NextMoveBusinessMessage("arkivmelding")
public class ArkivmeldingMessage extends BusinessMessage<ArkivmeldingMessage> {
}

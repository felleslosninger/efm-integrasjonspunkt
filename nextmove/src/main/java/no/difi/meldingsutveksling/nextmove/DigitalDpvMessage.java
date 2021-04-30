package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@NextMoveBusinessMessage("digital_dpv")
public class DigitalDpvMessage extends BusinessMessage<DigitalDpvMessage> {

    @NotNull
    private String tittel;
    @NotNull
    private String sammendrag;
    @NotNull
    private String innhold;

}

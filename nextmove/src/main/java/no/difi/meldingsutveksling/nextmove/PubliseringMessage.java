package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@NextMoveBusinessMessage("publisering")
public class PubliseringMessage extends BusinessMessage<PubliseringMessage> {
    @NotNull
    private String orgnr;
}

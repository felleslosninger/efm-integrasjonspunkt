package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@NextMoveBusinessMessage("innsynskrav")
public class InnsynskravMessage extends BusinessMessage<InnsynskravMessage> {
    @NotNull
    private String orgnr;
    @NotNull
    private String epost;
}

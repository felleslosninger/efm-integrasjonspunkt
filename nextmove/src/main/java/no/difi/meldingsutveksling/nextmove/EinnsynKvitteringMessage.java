package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@NextMoveBusinessMessage("einnsyn_kvittering")
public class EinnsynKvitteringMessage extends BusinessMessage<EinnsynKvitteringMessage> {

    @NotNull
    private String content;
}

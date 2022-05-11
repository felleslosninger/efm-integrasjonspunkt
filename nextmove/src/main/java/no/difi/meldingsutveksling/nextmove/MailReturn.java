package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class MailReturn extends AbstractEntity<Long> {

    @NotNull
    @Valid
    private PostAddress mottaker;

    @NotNull
    private ReturnHandling returhaandtering;
}

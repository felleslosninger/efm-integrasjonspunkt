package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

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

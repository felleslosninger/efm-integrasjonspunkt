package no.difi.meldingsutveksling.nextmove;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

@Data
public class DigitalPostInfo implements Serializable {

    @NotNull
    private LocalDate virkningsdato;
    @NotNull
    private Boolean aapningskvittering;
}

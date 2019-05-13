package no.difi.meldingsutveksling.nextmove;

import lombok.Data;

import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@Embeddable
public class DigitalPostInfo implements Serializable {

    @NotNull
    private LocalDate virkningsdato;
    @NotNull
    private Boolean aapningskvittering;
}

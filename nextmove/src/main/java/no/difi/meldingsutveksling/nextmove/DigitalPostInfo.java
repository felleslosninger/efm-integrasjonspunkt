package no.difi.meldingsutveksling.nextmove;

import lombok.Data;

import javax.persistence.Embeddable;
import java.time.LocalDate;

@Data
@Embeddable
public class DigitalPostInfo {

    private LocalDate virkningsdato;
    private Boolean aapningskvittering;
}

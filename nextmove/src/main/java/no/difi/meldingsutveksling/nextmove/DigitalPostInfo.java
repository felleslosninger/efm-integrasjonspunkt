package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import java.time.LocalDate;

@Getter
@Setter
@ToString
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class DigitalPostInfo {

    private LocalDate virkningsdato;
    private Boolean aapningskvittering;
}

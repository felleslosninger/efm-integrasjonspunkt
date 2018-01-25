package no.difi.meldingsutveksling.nextmove.dpi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.difi.meldingsutveksling.nextmove.Varsler;

import javax.persistence.Embeddable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class DigitalPostInfo {

    private String virkningsdato;
    private String virkningstidspunkt;
    private boolean aapningskvittering;
    private String ikkeSensitivTittel;
    private String language;
    private Varsler varsler;
}

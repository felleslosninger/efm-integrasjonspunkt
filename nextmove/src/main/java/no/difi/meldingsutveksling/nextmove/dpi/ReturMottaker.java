package no.difi.meldingsutveksling.nextmove.dpi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class ReturMottaker {

    private String navn;
    private String adresselinje1;
    private String adresselinje2;
    private String adresselinje3;
    private String postnummer;
    private String poststed;
    private String landkode;
    private String land;
}

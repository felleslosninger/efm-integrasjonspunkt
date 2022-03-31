
package no.difi.meldingsutveksling.dpi.client.domain.sbd;

import lombok.Data;

@Data
public class AdresseInformasjon {

    private String navn;
    private String adresselinje1;
    private String adresselinje2;
    private String adresselinje3;
    private String adresselinje4;
    private String postnummer;
    private String poststed;
    private String land;
    private String landkode;
}

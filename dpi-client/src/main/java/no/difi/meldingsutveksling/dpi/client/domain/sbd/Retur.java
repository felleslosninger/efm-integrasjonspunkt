
package no.difi.meldingsutveksling.dpi.client.domain.sbd;

import lombok.Data;


@Data
public class Retur {

    private AdresseInformasjon mottaker;
    private Returposthaandtering returposthaandtering;

    public enum Returposthaandtering {

        DIREKTE_RETUR,
        MAKULERING_MED_MELDING
    }
}

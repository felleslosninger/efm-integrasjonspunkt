
package no.difi.meldingsutveksling.dpi.client.domain.messagetypes;

import lombok.Data;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.AdresseInformasjon;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Avsender;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Dokumentpakkefingeravtrykk;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Retur;

@Data
public class Utskrift implements BusinessMessage, AvsenderHolder, DokumentpakkefingeravtrykkHolder, MaskinportentokenHolder {

    private Avsender avsender;
    private AdresseInformasjon mottaker;
    private Dokumentpakkefingeravtrykk dokumentpakkefingeravtrykk;
    private String maskinportentoken;

    private Utskriftstype utskriftstype;
    private Retur retur;
    private Posttype posttype;

    public enum Posttype {
        A,
        B
    }

    public enum Utskriftstype {
        SORT_HVIT,
        FARGE
    }
}

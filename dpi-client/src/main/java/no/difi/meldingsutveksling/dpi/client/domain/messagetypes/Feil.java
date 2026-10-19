
package no.difi.meldingsutveksling.dpi.client.domain.messagetypes;

import lombok.Data;

@Data
public class Feil extends AbstractKvittering implements MaskinportentokenHolder {

    public enum Type { KLIENT, SERVER }

    private String maskinportentoken;
    private Type feiltype;
    private String detaljer;

}

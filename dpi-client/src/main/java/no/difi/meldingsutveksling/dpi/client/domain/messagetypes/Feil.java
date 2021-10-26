
package no.difi.meldingsutveksling.dpi.client.domain.messagetypes;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Avsender;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Virksomhetmottaker;

import java.time.OffsetDateTime;

@Data
public class Feil implements BusinessMessage, MaskinportentokenHolder {

    private Avsender avsender;
    private Virksomhetmottaker mottaker;
    private String maskinportentoken;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime tidspunkt;
    private Type feiltype;
    private String detaljer;

    private enum Type {
        KLIENT, SERVER
    }
}

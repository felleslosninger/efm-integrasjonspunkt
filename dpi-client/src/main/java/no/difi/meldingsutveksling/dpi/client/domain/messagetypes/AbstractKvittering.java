package no.difi.meldingsutveksling.dpi.client.domain.messagetypes;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Avsender;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Virksomhetmottaker;

import java.time.OffsetDateTime;

@Data
abstract class AbstractKvittering implements Kvittering {

    private Avsender avsender;
    private Virksomhetmottaker mottaker;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime tidspunkt;
}

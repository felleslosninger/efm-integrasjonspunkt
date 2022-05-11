package no.difi.meldingsutveksling.dpi.client;

import lombok.Data;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Avsender;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Virksomhetmottaker;

@Data
public class ReceiptInput {

    private Virksomhetmottaker mottaker;
    private Avsender avsender;
}

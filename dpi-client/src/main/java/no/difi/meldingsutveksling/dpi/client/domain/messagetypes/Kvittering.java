package no.difi.meldingsutveksling.dpi.client.domain.messagetypes;


import no.difi.meldingsutveksling.dpi.client.domain.sbd.Virksomhetmottaker;

import java.time.OffsetDateTime;

public interface Kvittering extends BusinessMessage {

    Virksomhetmottaker getMottaker();

    OffsetDateTime getTidspunkt();
}

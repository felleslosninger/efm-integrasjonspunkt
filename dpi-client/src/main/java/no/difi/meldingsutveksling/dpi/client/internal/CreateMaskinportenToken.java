package no.difi.meldingsutveksling.dpi.client.internal;

import no.difi.meldingsutveksling.dpi.client.domain.sbd.Avsender;

public interface CreateMaskinportenToken {

    String createMaskinportenTokenForReceiving();

    String createMaskinportenTokenForSending(Avsender avsender);
}

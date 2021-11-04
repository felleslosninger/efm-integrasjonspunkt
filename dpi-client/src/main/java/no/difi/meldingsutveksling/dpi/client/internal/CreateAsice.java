package no.difi.meldingsutveksling.dpi.client.internal;

import no.difi.meldingsutveksling.dpi.client.domain.Shipment;

import java.io.OutputStream;

public interface CreateAsice {

    void createAsice(Shipment shipment, OutputStream outputStream);
}


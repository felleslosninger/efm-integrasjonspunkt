package no.difi.meldingsutveksling.dph.client.internal;

import no.difi.meldingsutveksling.domain.Iso6523;

public interface CreateMaskinportenToken {

    String createMaskinportenToken(Iso6523 onBehalfOf);
}

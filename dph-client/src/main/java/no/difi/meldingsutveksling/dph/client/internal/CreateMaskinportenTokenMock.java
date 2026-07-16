package no.difi.meldingsutveksling.dph.client.internal;


import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.Iso6523;

@RequiredArgsConstructor
public class CreateMaskinportenTokenMock implements CreateMaskinportenToken {

    private final String token;

    @Override
    public String createMaskinportenToken(Iso6523 onBehalfOf) {
        return token;
    }
}

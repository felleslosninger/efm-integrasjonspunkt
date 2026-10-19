package no.difi.meldingsutveksling.ks.svarut.rest;

import lombok.RequiredArgsConstructor;
import no.ks.svarut.klient.AuthenticationStrategy;
import org.eclipse.jetty.client.Request;
import org.jspecify.annotations.NonNull;

@RequiredArgsConstructor
class IntegrasjonspunktAuthenticationStrategy implements AuthenticationStrategy {

    private final String integrasjonId;
    private final String integrasjonPassord;
    private final GetSvarUtMaskinportenToken getSvarUtMaskinportenToken;

    @Override
    public void setAuthenticationHeaders(@NonNull Request request) {
        request.headers(headers ->
            headers
                .add("Authorization", "Bearer " + getMaskinportenToken())
                .add("IntegrasjonId", integrasjonId)
                .add("IntegrasjonPassord", integrasjonPassord)
        );
    }

    private String getMaskinportenToken() {
        return getSvarUtMaskinportenToken.createMaskinportenToken();
    }
}

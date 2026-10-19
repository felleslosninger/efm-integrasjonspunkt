package no.difi.meldingsutveksling.ks.svarut.rest;

import lombok.RequiredArgsConstructor;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenInput;
import org.springframework.cache.annotation.Cacheable;

@RequiredArgsConstructor
class GetSvarUtMaskinportenToken {

    private final ClientContext clientContext;
    private final JwtTokenClient jwtTokenClient;

    @Cacheable("svarUt.getMaskinportenToken")
    public String createMaskinportenToken() {
        JwtTokenInput input = new JwtTokenInput()
            .setConsumerOrg(clientContext.getSenderOrg());

        return jwtTokenClient.fetchToken(input).getAccessToken();
    }
}

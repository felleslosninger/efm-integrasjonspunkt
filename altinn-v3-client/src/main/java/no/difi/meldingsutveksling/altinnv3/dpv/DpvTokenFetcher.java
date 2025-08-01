package no.difi.meldingsutveksling.altinnv3.dpv;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.altinnv3.AltinnTokenSwapper;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenInput;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DpvTokenFetcher {
    private final IntegrasjonspunktProperties props;
    private final AltinnTokenSwapper tokenSwapper;
    @Qualifier("DpvJwtTokenClient")
    private final JwtTokenClient jwtTokenClient;

    @Cacheable(cacheNames = {"altinn.getDpvToken"})
    public String getToken(List<String> scopes){
        String maskinportenToken = jwtTokenClient.fetchToken(
                new JwtTokenInput()
                    .setClientId(props.getDpv().getOidc().getClientId())
                    .setScopes(scopes))
            .getAccessToken();

        return tokenSwapper.getAltinnToken(maskinportenToken, props.getDpv().getAltinnTokenExchangeUrl());
    }
}

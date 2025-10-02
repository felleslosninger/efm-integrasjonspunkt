package no.difi.meldingsutveksling.altinnv3.token;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.*;

@Service("DpoTokenProducer")
@RequiredArgsConstructor
public class DpoTokenProducer implements TokenProducer {

    private final IntegrasjonspunktProperties properties;
    private final TokenService tokenService;
    private final TokenExchangeService tokenExchangeService;

    @Override
    @Cacheable(cacheNames = {"altinn.getDpoToken"})
    public String produceToken(List<String> scopes) {

        var config = new TokenConfig(properties.getDpo().getOidc(), properties.getDpo().getAltinnTokenExchangeUrl());
        var authorizationClaims = ClaimsFactory.getAuthorizationClaims(properties.getDpo().getAuthorizationDetails());

        String maskinportenToken = tokenService.fetchToken(config, scopes, authorizationClaims);


        // TODO : dette fungerer både med maskinporten token og altinn token, men levetiden er ulik
        // det kan være en fordel å benytte altinn token da dette har mye lenger levetid enn maskinporten token
        // maskinporten token har levetid på 120 sekunder (2 min)
        // altinn token har levetid på 1800 sekunder (30 min)

        var altinnToken = tokenExchangeService.exchangeToken(maskinportenToken, config.exchangeUrl());
        return altinnToken;
    }
}

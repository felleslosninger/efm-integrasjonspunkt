package no.difi.meldingsutveksling.altinnv3.token;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.AltinnAuthorizationDetails;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DpoTokenProducer {

    private final IntegrasjonspunktProperties properties;
    private final TokenService tokenService;
    private final TokenExchangeService tokenExchangeService;

    @Cacheable(cacheNames = {"altinn.getDpoToken"})
    public String produceToken(AltinnAuthorizationDetails authorizationDetails, List<String> scopes) {

        var config = new TokenConfig(properties.getDpo().getOidc(), properties.getDpo().getAltinnTokenExchangeUrl());
        var authorizationClaims = ClaimsFactory.getAuthorizationClaims(authorizationDetails);

        String maskinportenToken = tokenService.fetchToken(config, scopes, authorizationClaims);


        // TODO : dette fungerer både med maskinporten token og altinn token, men levetiden er ulik
        // det kan være en fordel å benytte altinn token da dette har mye lenger levetid enn maskinporten token
        // maskinporten token har levetid på 120 sekunder (2 min)
        // altinn token har levetid på 1800 sekunder (30 min)

        var altinnToken = tokenExchangeService.exchangeToken(maskinportenToken, config.exchangeUrl());
        return altinnToken;
    }

}

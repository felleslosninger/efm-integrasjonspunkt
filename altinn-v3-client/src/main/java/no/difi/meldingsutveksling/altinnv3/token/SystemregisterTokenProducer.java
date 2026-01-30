package no.difi.meldingsutveksling.altinnv3.token;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("SystemregisterTokenProducer")
@RequiredArgsConstructor
public class SystemregisterTokenProducer implements TokenProducer {

    private final IntegrasjonspunktProperties properties;
    private final TokenService tokenService;
    private final TokenExchangeService tokenExchangeService;

    @Override
    @Cacheable(cacheNames = {"altinn.getSystemToken"})
    public String produceToken(List<String> scopes) {
        // We "misuse" OIDC configuration intended for DPO to fetch the token
        var config = new TokenConfig(properties.getDpo().getOidc(), properties.getDpo().getAltinnTokenExchangeUrl());
        String maskinPortenToken = tokenService.fetchToken(config, scopes, null);
        String altinnToken = tokenExchangeService.exchangeToken(maskinPortenToken, config.exchangeUrl());
        return altinnToken;
    }

}

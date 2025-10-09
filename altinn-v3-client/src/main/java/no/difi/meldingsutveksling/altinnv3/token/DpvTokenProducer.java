package no.difi.meldingsutveksling.altinnv3.token;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("DpvTokenProducer")
@RequiredArgsConstructor
public class DpvTokenProducer implements TokenProducer {

    private final IntegrasjonspunktProperties properties;
    private final TokenService tokenService;
    private final TokenExchangeService tokenExchangeService;

    @Override
    @Cacheable(cacheNames = {"altinn.getDpvToken"})
    public String produceToken(List<String> scopes) {
        var config = new TokenConfig(properties.getDpv().getOidc(), properties.getDpv().getAltinnTokenExchangeUrl());

        var token = tokenService.fetchToken(config, scopes, null);
        var altinnToken = tokenExchangeService.exchangeToken(token, config.exchangeUrl());

        return altinnToken;
    }
}

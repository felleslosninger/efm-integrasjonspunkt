package no.difi.meldingsutveksling.altinnv3.token;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

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
        String token = tokenService.fetchToken(config, scopes);
        return tokenExchangeService.exchangeToken(token, config.exchangeUrl());
    }

}

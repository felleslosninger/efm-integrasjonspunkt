package no.difi.meldingsutveksling.dph.client.internal;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenInput;
import org.springframework.cache.annotation.Cacheable;

@RequiredArgsConstructor
public class CreateMaskinportenTokenImpl implements CreateMaskinportenToken {

    private final JwtTokenClient jwtTokenClient;

    @Override
    @Cacheable("dphClient.getMaskinportenToken")
    public String createMaskinportenToken(Iso6523 onBehalfOf) {
        JwtTokenInput input = new JwtTokenInput()
            .setConsumerOrg(onBehalfOf.getOrganizationIdentifier());

        return jwtTokenClient.fetchToken(input).getAccessToken();
    }
}

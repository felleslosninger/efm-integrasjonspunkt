package no.difi.meldingsutveksling.dpi.client.internal;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Avsender;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenInput;
import org.springframework.cache.annotation.Cacheable;

@RequiredArgsConstructor
public class CreateMaskinportenTokenImpl implements CreateMaskinportenToken {

    private final JwtTokenClient jwtTokenClient;
    private final GetConsumerOrg getConsumerOrg;

    @Override
    @Cacheable("dpiClient.getMaskinportenToken")
    public String createMaskinportenTokenForReceiving() {
        return jwtTokenClient.fetchToken().getAccessToken();
    }

    @Override
    @Cacheable("dpiClient.getMaskinportenToken")
    public String createMaskinportenTokenForSending(Avsender avsender) {
        return jwtTokenClient.fetchToken(new JwtTokenInput()
                .setConsumerOrg(getConsumerOrg.getConsumerOrg(avsender))
        ).getAccessToken();
    }
}

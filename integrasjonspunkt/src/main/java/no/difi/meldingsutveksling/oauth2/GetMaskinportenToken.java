package no.difi.meldingsutveksling.oauth2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenConfig;
import org.springframework.cache.annotation.Cacheable;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class GetMaskinportenToken {

    private final JwtTokenClient jwtTokenClient;

    public GetMaskinportenToken(IntegrasjonspunktProperties props) {
        this.jwtTokenClient = jwtTokenClient(props);
    }

    private JwtTokenClient jwtTokenClient(IntegrasjonspunktProperties props) {
        JwtTokenConfig config = new JwtTokenConfig(
            props.getOidc().getClientId(),
            props.getOidc().getUrl().toString(),
            props.getOidc().getAudience(),
            getCurrentScopes(props),
            props.getOidc().getKeystore()
        );
        return new JwtTokenClient(config);
    }

    private List<String> getCurrentScopes(IntegrasjonspunktProperties props) {
        var scopeList = new ArrayList<String>();
        if (props.getFeature().isEnableDPO()) scopeList.add("eformidling:dpo");
        if (props.getFeature().isEnableDPE()) scopeList.add("eformidling:dpe");
        if (props.getFeature().isEnableDPV()) scopeList.add("eformidling:dpv");
        if (props.getFeature().isEnableDPF()) scopeList.add("eformidling:dpf");
        if (props.getFeature().isEnableDPI()) scopeList.addAll(List.of("eformidling:dpi",
            "digitalpostinnbygger:send",
            "global/kontaktinformasjon.read",
            "global/sikkerdigitalpost.read",
            "global/varslingsstatus.read",
            "global/sertifikat.read",
            "global/navn.read",
            "global/postadresse.read"));
        if (props.getFeature().isEnableDPH()) scopeList.add("eformidling:dph");
        return scopeList;
    }

    @Cacheable(cacheNames = {"MaskinportenTokenInterceptor.getMaskinportenToken"})
    public String getMaskinportenToken() {
        return jwtTokenClient.fetchToken().getAccessToken();
    }
}

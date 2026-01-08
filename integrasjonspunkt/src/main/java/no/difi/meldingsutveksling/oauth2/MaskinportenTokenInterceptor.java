package no.difi.meldingsutveksling.oauth2;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class MaskinportenTokenInterceptor implements ClientHttpRequestInterceptor {

    private final IntegrasjonspunktProperties props;

    public MaskinportenTokenInterceptor(IntegrasjonspunktProperties props) {
        this.props = props;
    }

    @NotNull
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        var accessToken = jwtTokenClient().fetchToken().getAccessToken();
        request.getHeaders().setBearerAuth(accessToken);
        return execution.execute(request, body);
    }

    private JwtTokenClient jwtTokenClient() {
        JwtTokenConfig config = new JwtTokenConfig(
            props.getOidc().getClientId(),
            props.getOidc().getUrl().toString(),
            props.getOidc().getAudience(),
            getCurrentScopes(),
            props.getOidc().getKeystore()
        );
        return new JwtTokenClient(config);
    }

    private List<String> getCurrentScopes() {
        var scopeList = new ArrayList<String>();
        if (props.getFeature().isEnableDPO()) scopeList.add("move/dpo.read");
        if (props.getFeature().isEnableDPE()) scopeList.add("move/dpe.read");
        if (props.getFeature().isEnableDPV()) scopeList.add("move/dpv.read");
        if (props.getFeature().isEnableDPF()) scopeList.add("move/dpf.read");
        if (props.getFeature().isEnableDPFIO()) scopeList.add("ks:fiks");
        if (props.getFeature().isEnableDPI()) scopeList.addAll(List.of("move/dpi.read",
            "digitalpostinnbygger:send",
            "global/kontaktinformasjon.read",
            "global/sikkerdigitalpost.read",
            "global/varslingsstatus.read",
            "global/sertifikat.read",
            "global/navn.read",
            "global/postadresse.read"));
        return scopeList;
    }

}

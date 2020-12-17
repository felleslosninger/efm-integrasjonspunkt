package no.difi.meldingsutveksling.auth;

import com.nimbusds.jose.proc.BadJWSException;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.config.OauthRestTemplateConfig;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import no.difi.move.common.config.KeystoreProperties;
import no.difi.move.common.oauth.JWTDecoder;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenConfig;
import no.difi.move.common.oauth.JwtTokenResponse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.client.RestOperations;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Ignore("Manual testing class")
public class OidcTokenClientTest {

    private IntegrasjonspunktProperties props;
    private JwtTokenConfig config;

    private List<String> scopes = Arrays.asList(
            "move/dpo.read",
            "move/dpe.read",
            "move/dpv.read",
            "move/dpi.read",
            "global/kontaktinformasjon.read",
            "global/sikkerdigitalpost.read",
            "global/varslingsstatus.read",
            "global/sertifikat.read",
            "global/navn.read",
            "global/postadresse.read");

    @Before
    @SneakyThrows
    public void setup() throws MalformedURLException {
        props = new IntegrasjonspunktProperties();
        props.setServiceregistryEndpoint("http://localhost:9099");

        props.setOidc(new IntegrasjonspunktProperties.Oidc());
        props.getOidc().setEnable(true);
        props.getOidc().setUrl(new URL("https://oidc-ver2.difi.no/idporten-oidc-provider/token"));
        props.getOidc().setAudience("https://oidc-ver2.difi.no/idporten-oidc-provider/");
        props.getOidc().setClientId("test_move");
        props.getOidc().setKeystore(new KeystoreProperties());
        props.getOidc().getKeystore().setLockProvider(false);
        props.getOidc().getKeystore().setAlias("client_alias");
        props.getOidc().getKeystore().setPassword("changeit");
        props.getOidc().getKeystore().setPath(new FileSystemResource("src/test/resources/kontaktinfo-client-test.jks"));

        props.setSign(new IntegrasjonspunktProperties.Sign());
        props.getSign().setEnable(true);
        props.getSign().setJwkUrl(new URL(props.getServiceregistryEndpoint()+ "/jwk"));

        props.setFeature(new IntegrasjonspunktProperties.FeatureToggle());
        props.getFeature().setEnableDPO(true);
        props.getFeature().setEnableDPE(true);
        props.getFeature().setEnableDPI(true);
        props.getFeature().setEnableDPV(true);

        config = new JwtTokenConfig(
                props.getOidc().getClientId(),
                props.getOidc().getUrl().toString(),
                props.getOidc().getAudience(),
                scopes,
                props.getOidc().getKeystore()
        );

    }

    @Test
    public void testGenJWT() throws ParseException {
        JwtTokenClient oidcTokenClient = new JwtTokenClient(config);
        String jwt = oidcTokenClient.generateJWT();
        System.out.println(jwt);
        SignedJWT parsedJWT = SignedJWT.parse(jwt);
        assertEquals("test_move", parsedJWT.getJWTClaimsSet().getIssuer());
        assertEquals(scopes.stream().reduce((a, b) -> a + " " + b).orElse(""), parsedJWT.getJWTClaimsSet().getClaims().get("scope"));
    }

    @Test
    @Ignore("Manual test")
    public void testTokenFetch() {
        JwtTokenClient oidcTokenClient = new JwtTokenClient(config);

        JwtTokenResponse response = oidcTokenClient.fetchToken();
        System.out.println(response.getAccessToken());
    }


    @Test
    @Ignore("Manual test")
    public void testOathRestTemplate() throws URISyntaxException, MalformedURLException, CertificateException, BadJWSException {
        JwtTokenClient oidcTokenClient = new JwtTokenClient(config);
        OauthRestTemplateConfig config = new OauthRestTemplateConfig(props);
        RestOperations ops = config.restTemplate(oidcTokenClient);
        RestClient restClient = new RestClient(props, ops, new JWTDecoder(), new URL(props.getServiceregistryEndpoint()).toURI());
        String response = restClient.getResource("identifier/06068700602");
        System.out.println(response);
    }

    @Test
    @Ignore("Manual test")
    public void testSasTokenFetch() throws URISyntaxException, IOException, CertificateException, BadJWSException {
        JwtTokenClient oidcTokenClient = new JwtTokenClient(config);
        OauthRestTemplateConfig config = new OauthRestTemplateConfig(props);
        RestOperations ops = config.restTemplate(oidcTokenClient);
        RestClient restClient = new RestClient(props, ops, new JWTDecoder(), new URL(props.getServiceregistryEndpoint()).toURI());
        String response = restClient.getResource("sastoken");
        System.out.println(response);
    }

}

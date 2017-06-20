package no.difi.meldingsutveksling.auth;

import com.nimbusds.jose.proc.BadJWSException;
import com.nimbusds.jwt.SignedJWT;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.config.OauthRestTemplateConfig;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
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
    public void setup() throws MalformedURLException {
        props = new IntegrasjonspunktProperties();
        props.setServiceregistryEndpoint("http://localhost:9099/");

        props.setOidc(new IntegrasjonspunktProperties.Oidc());
        props.getOidc().setEnable(true);
        props.getOidc().setUrl(new URL("https://oidc-ver2.difi.no/idporten-oidc-provider/token"));
        props.getOidc().setAudience("https://oidc-ver2.difi.no/idporten-oidc-provider/");
        props.getOidc().setClientId("test_move");
        props.getOidc().setKeystore(new IntegrasjonspunktProperties.Keystore());
        props.getOidc().getKeystore().setAlias("client_alias");
        props.getOidc().getKeystore().setPassword("changeit");
        props.getOidc().getKeystore().setPath(new FileSystemResource("src/test/resources/kontaktinfo-client-test.jks"));

        props.setSign(new IntegrasjonspunktProperties.Sign());
        props.getSign().setEnable(true);
        props.getSign().setCertificate(new FileSystemResource("src/test/resources/kontaktinfo-client.cer"));

        props.setFeature(new IntegrasjonspunktProperties.FeatureToggle());
        props.getFeature().setEnableDPO(true);
        props.getFeature().setEnableDPE(true);
        props.getFeature().setEnableDPI(true);
        props.getFeature().setEnableDPV(true);

    }

    @Test
    public void testGenJWT() throws ParseException {
        OidcTokenClient oidcTokenClient = new OidcTokenClient(props);
        String jwt = oidcTokenClient.generateJWT();
        System.out.println(jwt);
        SignedJWT parsedJWT = SignedJWT.parse(jwt);
        assertEquals("test_move", parsedJWT.getJWTClaimsSet().getIssuer());
        assertEquals(scopes.stream().reduce((a, b) -> a+" "+b).get(), parsedJWT.getJWTClaimsSet().getClaims().get("scope"));
    }

    @Test
    @Ignore("Manual test")
    public void testTokenFetch() {
        OidcTokenClient oidcTokenClient = new OidcTokenClient(props);

        IdportenOidcTokenResponse response = oidcTokenClient.fetchToken();
        System.out.println(response.getAccessToken());
    }


    @Test
    @Ignore("Manual test")
    public void testOathRestTemplate() throws InterruptedException, URISyntaxException {
        OidcTokenClient oidcTokenClient = new OidcTokenClient(props);
        OauthRestTemplateConfig config = new OauthRestTemplateConfig(props, oidcTokenClient);
        RestOperations ops = config.restTemplate();
        String response = ops.getForObject("http://localhost:9099/identifier/06068700602", String.class);
        System.out.println(response);
    }

    @Test
    @Ignore("Manual test")
    public void testSasTokenFetch() throws URISyntaxException, IOException, CertificateException, BadJWSException {
        OidcTokenClient oidcTokenClient = new OidcTokenClient(props);
        OauthRestTemplateConfig config = new OauthRestTemplateConfig(props, oidcTokenClient);
        RestOperations ops = config.restTemplate();
        RestClient restClient = new RestClient(props, ops);
        String response = restClient.getResource("sastoken");
        System.out.println(response);
    }

}

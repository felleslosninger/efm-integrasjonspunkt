package no.difi.meldingsutveksling.auth;

import com.nimbusds.jwt.SignedJWT;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.config.OauthRestTemplateConfig;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.client.RestOperations;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class OidcTokenClientTest {

    private IntegrasjonspunktProperties props;

    @Before
    public void setup() throws MalformedURLException {
        props = new IntegrasjonspunktProperties();

        props.setOidc(new IntegrasjonspunktProperties.Oidc());
        props.getOidc().setEnable(true);
        props.getOidc().setUrl(new URL("https://eid-exttest.difi.no/idporten-oidc-provider/token"));
        props.getOidc().setClientId("test_move");
        props.getOidc().setScopes(Arrays.asList("scope_move_1", "scope_move_2"));
        props.getOidc().setKeystore(new IntegrasjonspunktProperties.Keystore());
        props.getOidc().getKeystore().setAlias("client_alias");
        props.getOidc().getKeystore().setPassword("changeit");
        props.getOidc().getKeystore().setPath(new FileSystemResource("src/test/resources/kontaktinfo-client-test.jks"));

    }

    @Test
    public void testGenJWT() throws ParseException {
        OidcTokenClient oidcTokenClient = new OidcTokenClient(props);
        String jwt = oidcTokenClient.generateJWT();
        SignedJWT parsedJWT = SignedJWT.parse(jwt);
        assertEquals("test_move", parsedJWT.getJWTClaimsSet().getIssuer());
        assertEquals("scope_move_1 scope_move_2", parsedJWT.getJWTClaimsSet().getClaims().get("scope"));
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
        String response = ops.getForObject("http://localhost:9099/identifier/910075918", String.class);
        System.out.println(response);
    }


}

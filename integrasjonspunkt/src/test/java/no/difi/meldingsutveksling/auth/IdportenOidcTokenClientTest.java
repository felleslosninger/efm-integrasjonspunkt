package no.difi.meldingsutveksling.auth;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;

import java.net.MalformedURLException;
import java.net.URL;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore("trenger mock til test, gaar forelopig mot test-tjeneste")
public class IdportenOidcTokenClientTest {

    private static IntegrasjonspunktProperties propsMock;

    @BeforeClass
    public static void setup() throws MalformedURLException {
        propsMock = mock(IntegrasjonspunktProperties.class);
        IntegrasjonspunktProperties.Organization orgMock = mock(IntegrasjonspunktProperties.Organization.class);
        when(propsMock.getOrg()).thenReturn(orgMock);
        IntegrasjonspunktProperties.Keystore keyStoreMock = mock(IntegrasjonspunktProperties.Keystore.class);
        when(orgMock.getKeystore()).thenReturn(keyStoreMock);
        when(keyStoreMock.getPassword()).thenReturn("changeit");
        when(keyStoreMock.getAlias()).thenReturn("client_alias");
        when(keyStoreMock.getPath()).thenReturn(new FileSystemResource("src/test/resources/kontaktinfo-client-test.jks"));
        IntegrasjonspunktProperties.IdportenOidc oidcMock = mock(IntegrasjonspunktProperties.IdportenOidc.class);
        when(propsMock.getIdportenOidc()).thenReturn(oidcMock);
        when(oidcMock.getBaseUrl()).thenReturn(new URL("https://eid-exttest.difi.no"));
        when(oidcMock.getIssuer()).thenReturn("test_move");
    }

    @Test
    public void testJWT() {
        IdportenOidcTokenClient idportenOidcTokenClient = new IdportenOidcTokenClient(propsMock);

        IdportenOidcTokenResponse response = idportenOidcTokenClient.fetchToken("scope_move_1");
        System.out.println(response.getAccessToken());
    }
}

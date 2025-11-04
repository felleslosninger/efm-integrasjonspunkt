package no.difi.meldingsutveksling.altinnv3.dpo;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.altinnv3.UseFullTestConfiguration;
import no.difi.meldingsutveksling.altinnv3.token.AltinnConfiguration;
import no.difi.meldingsutveksling.altinnv3.token.DpoTokenProducer;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
@SpringBootTest(classes = {
    DpoTokenProducer.class,
    AltinnConfiguration.class,
    IntegrasjonspunktProperties.class,
})
@UseFullTestConfiguration
//@TestPropertySource(properties = {
//    "difi.move.dpo.oidc.authenticationType=JWK",
//    "difi.move.dpo.oidc.clientId=b590f149-d0ba-4fca-b367-bccd9e444a00",
//    "difi.move.dpo.authorizationDetails.systemuserOrgId=0192:311780735",
//    "difi.move.dpo.authorizationDetails.externalRef=311780735_integrasjonspunkt_systembruker_test3",
//    "difi.move.dpo.oidc.jwk.path=classpath:311780735-sterk-ulydig-hund-da.jwk"

//    "difi.move.dpo.oidc.authenticationType=CERTIFICATE",
//    "difi.move.dpo.oidc.clientId=a63cac91-3210-4c35-b961-5c7bf122345c",
//    "difi.move.dpo.authorizationDetails.systemuserOrgId=0192:991825827",
//    "difi.move.dpo.authorizationDetails.externalRef=991825827_integrasjonspunkt_systembruker_test3",
//    "difi.move.org.keystore.path=",
//    "difi.move.org.keystore.alias=",
//    "difi.move.org.keystore.password",
//})
public class DpoTokenFetcherTest {

    @Inject
    DpoTokenProducer dpoTokenProducer;

    @Inject
    IntegrasjonspunktProperties properties;

    @Test
    @Disabled("Manual test")
    void testAltinnToken() {
        var altinnToken = dpoTokenProducer.produceToken(
            properties.getDpo().getAuthorizationDetails(),
            List.of("altinn:broker.write", "altinn:broker.read")
        );
        assertNotNull(altinnToken, "AltinnToken is null");
    }

}

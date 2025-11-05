package no.difi.meldingsutveksling.altinnv3.dpv;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.altinnv3.UseFullTestConfiguration;
import no.difi.meldingsutveksling.altinnv3.token.AltinnConfiguration;
import no.difi.meldingsutveksling.altinnv3.token.DpvTokenProducer;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
@SpringBootTest(classes = {
    DpvTokenProducer.class,
    AltinnConfiguration.class,
    IntegrasjonspunktProperties.class,
})
@UseFullTestConfiguration
@TestPropertySource(properties = {
//    "difi.move.dpv.oidc.authenticationType=JWK",
//    "difi.move.dpv.oidc.clientId=b590f149-d0ba-4fca-b367-bccd9e444a00",
//    "difi.move.dpv.systemUser.orgId=0192:311780735",
//    "difi.move.dpv.systemUser.name=311780735_integrasjonspunkt_systembruker_test3",
//    "difi.move.dpv.oidc.jwk.path=classpath:311780735-sterk-ulydig-hund-da.jwk"

//    "difi.move.dpv.oidc.authenticationType=CERTIFICATE",
//    "difi.move.dpv.oidc.clientId=a63cac91-3210-4c35-b961-5c7bf122345c",
//    "difi.move.dpv.systemUser.orgId=0192:991825827",
//    "difi.move.dpv.systemUser.name=991825827_integrasjonspunkt_systembruker_test3",
//    "difi.move.org.keystore.path=",
//    "difi.move.org.keystore.alias=",
//    "difi.move.org.keystore.password",
})
public class DpvTokenFetcherTest {

    @Inject
    DpvTokenProducer dpvTokenProducer;

    @Test
    @Disabled("Manual test")
    void testAltinnToken() {
        var altinnToken = dpvTokenProducer.produceToken(List.of("altinn:broker.read"));
        assertNotNull(altinnToken, "AltinnToken is null");
    }

}

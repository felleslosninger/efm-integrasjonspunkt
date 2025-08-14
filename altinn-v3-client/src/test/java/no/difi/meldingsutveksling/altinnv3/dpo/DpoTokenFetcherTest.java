package no.difi.meldingsutveksling.altinnv3.dpo;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.altinnv3.UseFullTestConfiguration;
import no.difi.meldingsutveksling.altinnv3.token.AltinnConfiguration;
import no.difi.meldingsutveksling.altinnv3.token.DpoTokenProducer;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
@SpringBootTest(classes = {
    DpoTokenProducer.class,
    AltinnConfiguration.class,
    IntegrasjonspunktProperties.class,
})
@UseFullTestConfiguration
public class DpoTokenFetcherTest {

    @Inject
    DpoTokenProducer dpoTokenProducer;

    @Test
    @Disabled("Manual test")
    void testAltinnToken() {
        var altinnToken = dpoTokenProducer.produceToken(List.of("altinn:broker.write", "altinn:broker.read", "altinn:serviceowner"));
        assertNotNull(altinnToken, "AltinnToken is null");
    }

    /*
    {
  "iss": "eformidling-meldingsteneste-test",
  "aud": "https://test.maskinporten.no/",
  "exp": 1754399609,
  "iat": 1754399489,
  "jti": "edfdaafa-39a1-4035-b553-df7af6ee437b",
  "scope": "altinn:broker.read altinn:broker.write altinn:serviceowner"
}


{
  "iss": "a63cac91-3210-4c35-b961-5c7bf122345c",
  "aud": "https://test.maskinporten.no/",
  "exp": 1754399847,
  "iat": 1754399727,
  "jti": "29c3e9be-131d-4a67-a87e-f347f7fb47ca",
  "scope": "altinn:broker.write altinn:broker.read altinn:serviceowner"
}
     */


}

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

}

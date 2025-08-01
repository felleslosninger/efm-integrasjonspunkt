package no.difi.meldingsutveksling.altinnv3.dpo;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.altinnv3.AltinnConfiguration;
import no.difi.meldingsutveksling.altinnv3.UseFullTestConfiguration;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
@SpringBootTest(classes = {
    DpoTokenFetcher.class,
    AltinnConfiguration.class,
    IntegrasjonspunktProperties.class,
})
@UseFullTestConfiguration
public class DpoTokenFetcherTest {

    @Inject
    DpoTokenFetcher dpoTokenFetcher;

    @Test
    @Disabled("Manual test")
    void testAltinnToken() {
        var altinnToken = dpoTokenFetcher.getToken(List.of("altinn:broker.write", "altinn:broker.read", "altinn:serviceowner"));
        assertNotNull(altinnToken, "AltinnToken is null");
    }
}

package no.difi.meldingsutveksling.altinnv3;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
@SpringBootTest(classes = {
    AltinnConfig.class,
    AltinnTokenUtil.class,
    IntegrasjonspunktProperties.class
})
@ConfigurationPropertiesScan
public class AltinnTokenUtilTest {

    @Inject
    AltinnTokenUtil tokenUtil;

    @Test
    @Disabled("Manual test")
    void testAltinnToken() {
        var altinnToken = tokenUtil.retrieveAltinnAccessToken(List.of("altinn:broker.write", "altinn:broker.read", "altinn:serviceowner"));
        assertNotNull(altinnToken, "AltinnToken is null");
    }
}

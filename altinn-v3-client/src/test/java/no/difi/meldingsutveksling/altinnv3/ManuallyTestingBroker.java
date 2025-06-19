package no.difi.meldingsutveksling.altinnv3;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
@SpringBootTest(classes = {
    AltinnConfig.class,
    AltinnBroker.class,
    BrokerApiClient.class,
    AltinnTokenUtil.class,
    IntegrasjonspunktProperties.class
})
public class ManuallyTestingBroker {

    @Inject
    AltinnBroker broker;

    @Inject
    BrokerApiClient client;

    @Inject
    AltinnTokenUtil tokenUtil;

    @Test
    void testAltinnToken() {
        var altinnToken = tokenUtil.retrieveAltinnAccessToken("FIXMEt");
        assertNotNull(altinnToken, "AltinnToken is null");
    }

    @Test
    void testListFiles() throws Exception {
        var uuids = client.getAvailableFiles();
        assertNotNull(uuids);
        assertNotEquals(0, uuids.length);
        Arrays.stream(uuids).forEach(System.out::println);
    }

}

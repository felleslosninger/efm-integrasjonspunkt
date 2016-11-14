package no.difi.meldingsutveksling.ptp;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class DigitalPostInnbyggerConfigTest {
    @Test
    public void configFeatureShouldNotNull() throws Exception {
        DigitalPostInnbyggerConfig dpiConfig = new DigitalPostInnbyggerConfig();
        assertNotNull(dpiConfig.getFeature());
    }
}
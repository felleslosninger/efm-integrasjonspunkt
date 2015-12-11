package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRequiredPropertyException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = IntegrasjonspunktConfiguration.class)
@ActiveProfiles("dev")
@SuppressWarnings("SpringJavaAutowiredMembersInspection")
@Ignore("Temporary ignored due to validation. Need to swap out old configuration before enabling again.")
public class IntegrasjonspunktConfigurationDevIntegrationTest {
    @Autowired
    private Environment environment;

    private IntegrasjonspunktConfiguration configuration;

    @Before
    public void setUp() throws MeldingsUtvekslingRequiredPropertyException {
        configuration = new IntegrasjonspunktConfiguration(environment);
    }

    @Test
    public void shouldHaveDevProfileWhenNoProfileIsGiven() throws MeldingsUtvekslingRequiredPropertyException {
        assertEquals("dev", configuration.getProfile());
    }

    @Test
    public void shouldHaveEndpointsEnabledWhenDevEnvironment() {
        assertEquals("true", environment.getProperty("endpoints.enabled"));
    }

    @Test
    public void shouldHaveDisabledSecurityWhenDevEnvironment() {
        assertEquals("false", environment.getProperty("security.basic.enabled"));
    }

    @Test
    public void shouldHaveDisabledHealthSensitiveInformationWhenDevEnvironment() {
        assertEquals("false", environment.getProperty("endpoints.health.sensitive"));
    }

    @Test
    public void shouldHaveEnabledQueueFeatureToggleWhenDevEnvironment() {
        assertEquals(true, configuration.isQueueEnabled());
    }

    @Test
    public void shouldHaveOrganizationNumberWhenDevEnvironment() {
        assertEquals(true, configuration.hasOrganisationNumber());
    }

    @Test
    public void shouldHaveDevAltinnUsernameDefaultWhenDevEnvironment() {
        assertNotNull(configuration.getAltinnUsername());
    }

    @Test
    public void shouldHaveDevAltinnPasswordDefaultWhenDevEnvironment() {
        assertNotNull(configuration.getAltinnPassword());
    }

    @Test
    public void shouldHaveAltinnExternalServiceCodeForTestWhenDevEnvironment() {
        assertEquals("4192", configuration.getAltinnServiceCode());
    }

    @Test
    public void shouldHaveAltinnExternalServiceEditionCodeForTestWhenDevEnvironment() {
        assertEquals("270815", configuration.getAltinnServiceEditionCode());
    }
}

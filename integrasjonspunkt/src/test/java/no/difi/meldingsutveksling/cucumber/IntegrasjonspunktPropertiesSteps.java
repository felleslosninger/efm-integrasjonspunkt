package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RequiredArgsConstructor
public class IntegrasjonspunktPropertiesSteps {

    private final IntegrasjonspunktProperties propertiesSpy;

    @After
    public void after() {
        reset(propertiesSpy);
    }

    @Given("^the DPF feature is enabled$")
    public void theDPFFeatureIsEnabled() {
        given(propertiesSpy.getFeature().isEnableDPF()).willReturn(true);
    }
}

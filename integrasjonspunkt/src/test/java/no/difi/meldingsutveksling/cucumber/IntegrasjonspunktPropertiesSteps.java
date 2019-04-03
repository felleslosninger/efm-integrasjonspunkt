package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.After;
import cucumber.api.java.en.Given;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.mockito.Mockito;

import static org.mockito.BDDMockito.given;

@RequiredArgsConstructor
public class IntegrasjonspunktPropertiesSteps {

    private final IntegrasjonspunktProperties integrasjonspunktProperties;

    @After
    public void after() {
        Mockito.reset(integrasjonspunktProperties.getNoarkSystem());
    }

    @Given("^the Noark System is enabled$")
    public void theNoarkSystemIsEnabled() {
        given(integrasjonspunktProperties.getNoarkSystem().isEnable()).willReturn(true);
    }
}

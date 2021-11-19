package no.difi.meldingsutveksling.cucumber;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import org.mockito.Mockito;

import static org.mockito.BDDMockito.given;

@RequiredArgsConstructor
public class IntegrasjonspunktPropertiesSteps {

    private final IntegrasjonspunktProperties integrasjonspunktProperties;
    private final NoarkClient noarkClient;

    @After
    public void after() {
        Mockito.reset(integrasjonspunktProperties.getNoarkSystem());
        Mockito.reset(noarkClient.getNoarkClientSettings());
    }

    @Given("^the Noark System is enabled$")
    public void theNoarkSystemIsEnabled() {
        given(integrasjonspunktProperties.getNoarkSystem().getEndpointURL()).willReturn("http://localhost:8088/testExchangeBinding");
        given(integrasjonspunktProperties.getNoarkSystem().getType()).willReturn("p360");
        given(noarkClient.getNoarkClientSettings().getEndpointUrl()).willReturn("http://localhost:8088/testExchangeBinding");
    }

    @Given("^the Noark System is disabled$")
    public void theNoarkSystemIsDisabled() {
        given(integrasjonspunktProperties.getNoarkSystem().getType()).willReturn("");
    }
}

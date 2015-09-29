package difi.steps;

import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import difi.TestClient;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class KanMottaMeldingSteps {
    int orgnummer;
    boolean result;
    private CompositeConfiguration configuration;

    @Before
    public void setup() throws ConfigurationException {
        configuration = new CompositeConfiguration();
        configuration.addConfiguration(new SystemConfiguration());
        configuration.addConfiguration(new PropertiesConfiguration("test-configuration.properties"));
    }

    @Given("^en mottakende organisasjon med organisasjonsnummer (\\d+)$")
    public void en_mottakende_organisasjon_med_organisasjonsnummer_organisasjonsnummer(int orgnummer) throws Throwable {
        this.orgnummer = orgnummer;
    }

    @When("^vi sjekker om mottaker kan motta melding$")
    public void vi_sjekker_om_mottaker_kan_motta_meldinger() throws Throwable {
        TestClient integrasjonspunkt = new TestClient(configuration.getString("integrasjonspunkt.url"));
        result = integrasjonspunkt.canGetRecieveMessage(orgnummer);
    }

    @Then("^skal vi få (.+) i svar$")
    public void skal_vi_få_resultat_i_svar(String svar) throws Throwable {
        if(svar.equals("sann")) {
            assertTrue(result);
        } else {
            assertFalse(result);
        }

    }
}

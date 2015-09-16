package difi.steps;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import difi.TestClient;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class KanMottaMeldingSteps {
    int orgnummer;
    boolean result;

    @Given("^en mottakende organisasjon med organisasjonsnummer (\\d+)$")
    public void en_mottakende_organisasjon_med_organisasjonsnummer_organisasjonsnummer(int orgnummer) throws Throwable {
        // Express the Regexp above with the code you wish you had
        this.orgnummer = orgnummer;
    }

    @When("^vi sjekker om mottaker kan motta melding$")
    public void vi_sjekker_om_mottaker_kan_motta_meldinger() throws Throwable {
        // Express the Regexp above with the code you wish you had
        TestClient integrasjonspunkt = new TestClient();
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

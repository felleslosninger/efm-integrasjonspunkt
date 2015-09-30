package difi.steps;

import cucumber.api.PendingException;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class MottaMeldingSteps {
    private String partyNumber;

    @Given("^Altinn har melding til mottaker (\\d+) som mottaker finner$")
    public void altinn_har_melding_til_mottaker_som_mottaker_finner(String partynumber) throws Throwable {
        this.partyNumber = partynumber;
        throw new PendingException();
    }

    @When("^mottaker sjekker etter nye meldinger$")
    public void mottaker_sjekker_etter_nye_meldinger() throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    @Then("^mottaker mottar liste over nye meldinger$")
    public void mottaker_mottar_liste_over_nye_meldinger() throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    @Given("^Altinn har ikke melding til mottaker (\\d+)$")
    public void Altinn_har_ikke_melding_til_mottaker(String partyNumber) throws Throwable {
        this.partyNumber = partyNumber;
        throw new PendingException();
    }

    @Then("^mottaker mottar tom liste$")
    public void mottaker_mottar_tom_liste() throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    @When("^mottaker mottar melding$")
    public void mottaker_mottar_melding() throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new PendingException();
    }

    @Given("^ny <meldingsformat> melding$")
    public void ny_meldingsformat_melding() throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new PendingException();
    }

    @Then("^arkivsystem skal lagre melding$")
    public void arkivsystem_skal_lagre_melding() throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new PendingException();
    }
}

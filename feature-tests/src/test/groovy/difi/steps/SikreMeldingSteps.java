package difi.steps;

import cucumber.api.PendingException;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.And;
import cucumber.api.java.en.When;
import cucumber.api.java.en.Then;
import difi.CertificatesClient;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;

/**
 * Created by vat on 25.09.2015.
 */
public class SikreMeldingSteps {

    private String soapResponse;
    private String bestEduMessage;
    private String sender, reciever;
    private CompositeConfiguration configuration;
    private CertificatesClient client;

    @Before
    public void setup() throws ConfigurationException {
        this.configuration = new CompositeConfiguration();
        configuration.addConfiguration(new SystemConfiguration());
        configuration.addConfiguration(new PropertiesConfiguration("test-configuration.properties"));
        this.client = new CertificatesClient(configuration.getString("adresseregister.url"));

    }

    @Given("^avsender med orgnummer (\\d+) har gyldig sertifikat$")
    public void avsender_med_orgnummer_har_gyldig_sertifikat(int arg1) throws Throwable {
        // Express the Regexp above with the code you wish you had

        throw new PendingException();
    }

    @And("^mottaker med orgnummer (\\d+) har gyldig sertifikat$")
    public void mottaker_med_orgnummer_har_gyldig_sertifikat(int arg1) throws Throwable {
        // Express the Regexp above with the code you wish you had

        throw new PendingException();
    }

    @When("^vi sender melding$")
    public void vi_sender_melding() throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new PendingException();
    }

    @Then("^vi skal få svar om at melding har blitt formidlet$")
    public void vi_skal_få_svar_om_at_melding_har_blitt_formidlet() throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new PendingException();
    }
}

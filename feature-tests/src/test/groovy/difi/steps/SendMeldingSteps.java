package difi.steps;


import cucumber.api.PendingException;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import difi.BestEduTestMessageFactory;
import difi.CertificatesClient;
import difi.TestClient;
import groovy.lang.Writable;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

import static org.junit.Assert.assertEquals;

public class SendMeldingSteps {

    public static final String STALHEIM = "910094092";
    private String soapResponse;
    private String bestEduMessage;
    private String sender, reciever;
    private CompositeConfiguration configuration;

    @Before
    public void setup() throws ConfigurationException {
        this.configuration = new CompositeConfiguration();
        configuration.addConfiguration(new SystemConfiguration());
        configuration.addConfiguration(new PropertiesConfiguration("test-configuration.properties"));
    }

    @Given("^en velformert melding med mottaker Stålheim$")
    public void en_velformert_melding_med_mottaker_Stålheim() throws Throwable {
        Writable message = BestEduTestMessageFactory.createMessage(128);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.writeTo(new BufferedWriter(new OutputStreamWriter(outputStream)));
        bestEduMessage = message.toString();
        reciever = configuration.getString("stalheim.partynumber");
        sender = ""; // this value comes from local properties on the integration point
    }

    @Given("^mottaker finnes i adresseregister$")
    public void mottaker_finnes_i_adresseregister() throws Throwable {
        String certificate = "-----BEGIN CERTIFICATE-----\nMIID/DCCAuSgAwIBAgIENA0fKzANBgkqhkiG9w0BAQsFADBeMRIwEAYDVQQKEwlEaWZpIHRlc3Qx\nEjAQBgNVBAUTCTk5MTgyNTgyNzE0MDIGA1UEAxMrRElGSSB0ZXN0IHZpcmtzb21oZXRzc2VydGlm\naWF0IGludGVybWVkaWF0ZTAeFw0xNTAxMDEyMzQ2MzNaFw0xNzAyMDEyMzQ2MzNaMD0xEjAQBgNV\nBAUTCTk4NzQ2NDI5MTEnMCUGA1UEAxMeRElGSSB0ZXN0IHZpcmtzb21oZXRzc2VydGlmaWF0MIIB\nIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzsS7f45nBO8bL+nQ/oxUHEaSRg+zhwWZ8HVt\nFucoXi+hLt89IFM/OiC9YYaidjGo8P+C4FfDzeUJolsApeQ3c0I94Uhz+MICi0GgPYKbyw9EPMTD\nmuCkjbaMk3e+EbuzDiE2usYNWtIpzdRgqPTxOXToBydD4qFx8rLOfTzRqufTOD85xLTfKm0tlibM\n25z4pf9FMgPnZ8c735EN/Pe7ok2uVpnWDj9YlESGJyhUeQJKZotNsGILAm6o5hNWBUh7bY18rDiG\nZjPjZ36JH0sQRsITRy3Nhc/KpxkDMqXY2LcotMM8XoilI/YKkhJvg/e0qYT6fnFcDaU46hzYVSn9\nwwIDAQABo4HiMIHfMIGLBgNVHSMEgYMwgYCAFBwaROx+jMV17T8nbBo+6Vf0hn38oWKkYDBeMRIw\nEAYDVQQKEwlEaWZpIHRlc3QxEjAQBgNVBAUTCTk5MTgyNTgyNzE0MDIGA1UEAxMrRElGSSB0ZXN0\nIHZpcmtzb21oZXRzc2VydGlmaWF0IGludGVybWVkaWF0ZYIEIbToeDAdBgNVHQ4EFgQU1Nh6rWdb\n8KkNiKfOcQjiAfuGJCEwCQYDVR0TBAIwADAVBgNVHSAEDjAMMAoGCGCEQgEBAQFkMA4GA1UdDwEB\n/wQEAwIEsDANBgkqhkiG9w0BAQsFAAOCAQEAAcMiUkXq1IO3M/wU1YbdGr6+2dhsgxKaGeDt7bl1\nefjyENXI6dM2dspfyVI+/deIqX7VW/ay8AqiNJyFlvA9CMxW51+FivdjGENzRAKGF3pFsvdwNBEw\nFQSZCYoo8/gm59SidmnPNFeziUsE3fbQ22BPxW3l8ScSbYhgLlK9Tkr/ul3h7ByVtUdolP99eyCp\n1/TgC8EBZHZRC1v221+0AQ09A/SI/gyomgCeXVfH1Ll08v7BCTE1nE1aUqMDpDjOeWc73+f2X6vb\nUQdK4QwRU+pl5Oz6QgAFZ2mOD6DmqRfVoibM9sWgCkO5t6lpW86E/wixZBfS9TW/RJgH7461gg==\n-----END CERTIFICATE-----\n";
        CertificatesClient client = new CertificatesClient(configuration.getString("adresseregister.url"));
        if(!client.exists(reciever)) {
            client.addOrganization(reciever, certificate);
        }
    }

    @Then("^Vi skal få beskjed om at mottaker ikke kan motta meldinger$")
    public void vi_skal_få_beskjed_om_at_mottaker_ikke_kan_motta_meldinger() throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    @When("^vi sender dokumentet til Røn sitt integrasjonspunkt$")
    public void vi_sender_dokumentet_til_Røn_sitt_integrasjonspunkt() throws Throwable {
        // Express the Regexp above with the code you wish you had
        TestClient testClient = new TestClient(configuration.getString("ron.integrasjonspunkt.url"));
        soapResponse = testClient.putMessage(sender, reciever, bestEduMessage);
    }

    @Then("^dokumentet blir videreformidlet til Stålheim$")
    public void dokumentet_blir_videreformidlet_til_Stålheim() throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new PendingException("Check if Stålheim has the message in Altinn?");
    }

    @And("^melding om at dokument er sendt videre returneres til SAK/ARKIV$")
    public void melding_om_at_dokument_er_sendt_videre_returneres_til_SAK_ARKIV() throws Throwable {
        // Express the Regexp above with the code you wish you had
        assertEquals("OK", soapResponse);
    }

    @Given("^SBD dokument skal videresendes til Altinn$")
    public void SBD_dokument_skal_videresendes_til_Altinn() throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new PendingException();
    }

    @And("^Altinn er nede$")
    public void Altinn_er_nede() throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new PendingException("Hvordan simulere at Altinn er nede?");
    }

    @Then("^feilmelding returneres tilbake til SAK/ARKIV$")
    public void feilmelding_returneres_tilbake_til_SAK_ARKIV() throws Throwable {
        // Express the Regexp above with the code you wish you had
        assertEquals("ERROR", soapResponse);
    }

    @Given("^BEST/EDU dokument fra Fylkesmannen i Sogn og Fjordane$")
    public void BEST_EDU_dokument_fra_Fylkesmannen_i_Sogn_og_Fjordane() throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new PendingException();
    }

    @Then("^JournalpostId skal finnes i SBD dokumentet$")
    public void JournalpostId_skal_finnes_i_SBD_dokumentet() throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new PendingException("Hvordan kan vi verifisere SBD dokumentet?");
    }

    @Given("^BEST/EDU dokument fra public 360$")
    public void BEST_EDU_dokument_fra_public() throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new PendingException();
    }

    @Given("^avsender med ugyldig virksomhetssertifikat$")
    public void avsender_med_ugyldig_virksomhetssertifikat() throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new PendingException("Bruker certificates client til å legge inn et ugyldig virksomhetssertifikat");
    }

    @And("^mottaker med ugyldig virksomhetssertifikat$")
    public void mottaker_med_ugyldig_virksomhetssertifikat() throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new PendingException("Bruker certificates client til å legge inn et ugyldig virksomhetssertifikat");
    }

    @Then("^dokumentet blir ikke sendt$")
    public void dokumentet_blir_ikke_sendt() throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new PendingException("Kalle på Altinn og sjekke at dokumentet ikke ligger i mottakers filer");
    }
}

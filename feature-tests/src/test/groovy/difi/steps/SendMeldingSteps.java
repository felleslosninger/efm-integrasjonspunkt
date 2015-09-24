package difi.steps;

import cucumber.api.PendingException;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import difi.BestEduTestMessageFactory;
import difi.CertificatesClient;
import difi.TestClient;
import groovy.lang.Writable;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

import static org.junit.Assert.assertEquals;

public class SendMeldingSteps {

    private String soapResponse;
    private String bestEduMessage;
    private String sender, reciever;

    @Given("^en melding med mottaker (\\d+)$")
    public void en_melding_med_mottaker(String arg1) throws Throwable {
        BestEduTestMessageFactory messageFactory = new BestEduTestMessageFactory();
        Writable message = messageFactory.createMessage(sender, arg1, 128);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.writeTo(new BufferedWriter(new OutputStreamWriter(outputStream)));
        bestEduMessage = message.toString();
        reciever = arg1;
        sender = ""; // this value comes from local properties on the integration point
    }

    @When("^vi skal sende melding$")
    public void vi_skal_sende_melding() throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    @Then("^Vi skal få beskjed om at mottaker ikke kan motta meldinger$")
    public void vi_skal_få_beskjed_om_at_mottaker_ikke_kan_motta_meldinger() throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    @Given("^mottaker finnes i adresseregister$")
    public void mottaker_finnes_i_adresseregister() throws Throwable {
        String certificate = "-----BEGIN CERTIFICATE-----\nMIID/DCCAuSgAwIBAgIENA0fKzANBgkqhkiG9w0BAQsFADBeMRIwEAYDVQQKEwlEaWZpIHRlc3Qx\nEjAQBgNVBAUTCTk5MTgyNTgyNzE0MDIGA1UEAxMrRElGSSB0ZXN0IHZpcmtzb21oZXRzc2VydGlm\naWF0IGludGVybWVkaWF0ZTAeFw0xNTAxMDEyMzQ2MzNaFw0xNzAyMDEyMzQ2MzNaMD0xEjAQBgNV\nBAUTCTk4NzQ2NDI5MTEnMCUGA1UEAxMeRElGSSB0ZXN0IHZpcmtzb21oZXRzc2VydGlmaWF0MIIB\nIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzsS7f45nBO8bL+nQ/oxUHEaSRg+zhwWZ8HVt\nFucoXi+hLt89IFM/OiC9YYaidjGo8P+C4FfDzeUJolsApeQ3c0I94Uhz+MICi0GgPYKbyw9EPMTD\nmuCkjbaMk3e+EbuzDiE2usYNWtIpzdRgqPTxOXToBydD4qFx8rLOfTzRqufTOD85xLTfKm0tlibM\n25z4pf9FMgPnZ8c735EN/Pe7ok2uVpnWDj9YlESGJyhUeQJKZotNsGILAm6o5hNWBUh7bY18rDiG\nZjPjZ36JH0sQRsITRy3Nhc/KpxkDMqXY2LcotMM8XoilI/YKkhJvg/e0qYT6fnFcDaU46hzYVSn9\nwwIDAQABo4HiMIHfMIGLBgNVHSMEgYMwgYCAFBwaROx+jMV17T8nbBo+6Vf0hn38oWKkYDBeMRIw\nEAYDVQQKEwlEaWZpIHRlc3QxEjAQBgNVBAUTCTk5MTgyNTgyNzE0MDIGA1UEAxMrRElGSSB0ZXN0\nIHZpcmtzb21oZXRzc2VydGlmaWF0IGludGVybWVkaWF0ZYIEIbToeDAdBgNVHQ4EFgQU1Nh6rWdb\n8KkNiKfOcQjiAfuGJCEwCQYDVR0TBAIwADAVBgNVHSAEDjAMMAoGCGCEQgEBAQFkMA4GA1UdDwEB\n/wQEAwIEsDANBgkqhkiG9w0BAQsFAAOCAQEAAcMiUkXq1IO3M/wU1YbdGr6+2dhsgxKaGeDt7bl1\nefjyENXI6dM2dspfyVI+/deIqX7VW/ay8AqiNJyFlvA9CMxW51+FivdjGENzRAKGF3pFsvdwNBEw\nFQSZCYoo8/gm59SidmnPNFeziUsE3fbQ22BPxW3l8ScSbYhgLlK9Tkr/ul3h7ByVtUdolP99eyCp\n1/TgC8EBZHZRC1v221+0AQ09A/SI/gyomgCeXVfH1Ll08v7BCTE1nE1aUqMDpDjOeWc73+f2X6vb\nUQdK4QwRU+pl5Oz6QgAFZ2mOD6DmqRfVoibM9sWgCkO5t6lpW86E/wixZBfS9TW/RJgH7461gg==\n-----END CERTIFICATE-----\n";
        CertificatesClient client = new CertificatesClient("http://localhost:9999/");
        if(!client.exists(reciever)) {
            client.addOrganization(reciever, certificate);
        }
    }

    @When("^vi sender melding$")
    public void vi_sender_melding() throws Throwable {
        TestClient testClient = new TestClient();
        soapResponse = testClient.putMessage(sender, reciever, bestEduMessage);
    }

    @Then("^vi skal få svar om at melding har blitt formidlet$")
    public void vi_skal_få_svar_om_at_melding_har_blitt_formidlet() throws Throwable {
        assertEquals("OK", soapResponse);
    }

    @And("^mottaker finnes ikke i adresseregisteret$")
    public void mottaker_finnes_ikke_i_adresseregisteret() throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new PendingException();
    }

}

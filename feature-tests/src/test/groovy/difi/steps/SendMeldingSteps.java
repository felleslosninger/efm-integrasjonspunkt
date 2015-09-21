package difi.steps;

import cucumber.api.PendingException;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import difi.BestEduTestMessageFactory;
import difi.TestClient;
import groovy.lang.Writable;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

public class SendMeldingSteps {

    private String mottaker;

    @Given("^en melding med mottaker (\\d+)$")
    public void en_melding_med_mottaker(String arg1) throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        BestEduTestMessageFactory messageFactory = new BestEduTestMessageFactory();
        Writable message = messageFactory.createMessage(sender, arg1, 128);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.writeTo(new BufferedWriter(new OutputStreamWriter(outputStream)));
        bestEduMessage = outputStream.toByteArray();
        mottaker = arg1;
        sender = "974720760";
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
        // Write code here that turns the phrase above into concrete actions
        //
    }

    @When("^vi sender melding$")
    public void vi_sender_melding() throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        TestClient testClient = new TestClient();
        testClient.putMessage(sender, reciever, bestEduMessage);
    }

    @Then("^vi skal få svar om at melding har blitt formidlet$")
    public void vi_skal_få_svar_om_at_melding_har_blitt_formidlet() throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    @And("^mottaker finnes ikke i adresseregisteret$")
    public void mottaker_finnes_ikke_i_adresseregisteret() throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new PendingException();
    }
    private byte[] bestEduMessage;
    private String sender, reciever;
    
    public void en_melding_på_MB(int meldingStoerrelse) throws Throwable {
        // Express the Regexp above with the code you wish you had
        BestEduTestMessageFactory messageFactory = new BestEduTestMessageFactory();
        Writable message = messageFactory.createMessage(sender, reciever, 128);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.writeTo(new BufferedWriter(new OutputStreamWriter(outputStream)));
        bestEduMessage = outputStream.toByteArray();

        //throw new PendingException("Opprett BEST/EDU melding på " + meldingStoerrelse + " MB");

    }

}

package difi.steps;

import cucumber.api.PendingException;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import difi.BestEduTestMessageFactory;
import groovy.lang.Writable;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

public class SendMeldingSteps {
    private byte[] bestEduMessage;
    private String sender, reciever;
    @Given("^en melding på (\\d+) MB$")
    public void en_melding_på_MB(int meldingStoerrelse) throws Throwable {
        // Express the Regexp above with the code you wish you had
        BestEduTestMessageFactory messageFactory = new BestEduTestMessageFactory();
        sender = "974720760";
        reciever = "974720760";
        Writable message = messageFactory.createMessage(sender, reciever, 128);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.writeTo(new BufferedWriter(new OutputStreamWriter(outputStream)));
        bestEduMessage = outputStream.toByteArray();

        //throw new PendingException("Opprett BEST/EDU melding på " + meldingStoerrelse + " MB");

    }

    @When("^integrasjonspunkt mottar meldingen$")
    public void integrasjonspunkt_mottar_meldingen() throws Throwable {
        // Express the Regexp above with the code you wish you had

        //TestClient integrasjonspunkt = new TestClient();
        //SOAPResponse soapResponse = integrasjonspunkt.putMessage(sender, reciever, bestEduMessage);
        throw new PendingException("Kalle på putMessage i integrasjonspunktet");
    }

    @Then("^meldingen blir sent til Altinn$")
    public void meldingen_blir_sent_til_Altinn() throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new PendingException("Skrive kode for å sjekke at returverdi fra integrasjonspunkt viser at melding ble sendt til Altinn");
    }

    @And("^integrasjonspunkt returnerer Ok$")
    public void integrasjonspunkt_returnerer_Ok() throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new PendingException("Skrive kode for å sjekke at returverdi fra integrasjonspunkt er Ok");
    }
}

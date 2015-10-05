package no.difi.meldingsutveksling.noarkexchange;

import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.schema.*;
import no.difi.meldingsutveksling.services.AdresseregisterService;


import org.mockito.Mockito;

import static junit.framework.TestCase.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;

/**
 * Makes sure that the integrasjonspunkt can handle receipt messages on the receive interface
 *
 * @author Glenn Bech
 */

public class ReceiveSteps {

    private IntegrasjonspunktImpl integrasjonspunkt;
    private AdresseregisterService adresseregister ;
    private EventLog eventLog ;
    private PutMessageRequestType message;

    @Before
    public void setup() {
        integrasjonspunkt = new IntegrasjonspunktImpl();
        adresseregister = Mockito.mock(AdresseregisterService.class);
        eventLog = Mockito.mock(EventLog.class);
        integrasjonspunkt.setEventLog(eventLog);

        MessageSender messageSender = new MessageSender();
        messageSender.setAdresseregister(adresseregister);
        messageSender.setEventLog(eventLog);
        integrasjonspunkt.setMessageSender(messageSender);
  }

    @Given("^et dokument mottatt på integrasjospunktet sitt receive grensesnitt er en kvittering$")
    public void et_dokument_mottatt_på_receive_grensesnittet() {

        // envelope
        message = new PutMessageRequestType();
        EnvelopeType envelope = new EnvelopeType();
        AddressType sender = new AddressType();
        sender.setOrgnr("9874642");
        envelope.setSender(sender);

        AddressType receiver = new AddressType();
        receiver.setOrgnr("9874643");
        envelope.setReceiver(receiver);
        message.setEnvelope(envelope);

        //body
        AppReceiptType receiptType = new AppReceiptType() ;
        receiptType.setType("OK");
        StatusMessageType statusMessage =  new StatusMessageType();
        statusMessage.setCode("ID");
        statusMessage.setText("210725");
        receiptType.getMessage().add(statusMessage);
        message.setPayload(receiptType);

    }

    @When("^integrasjonspunktet mottar et dokument på receive rensesnit$")
    public void vi_skal_sende_melding(){
        integrasjonspunkt.putMessage(message);

    }

    @Then("^kvitteringen logges i integrasjonspunktet sin hendelseslogg$")
    public void kvitteringen_logges_i_eventlog()  {
        // Write code here that turns the phrase above into concrete actions
        Mockito.verify(eventLog, times(1)).log(any(Event.class));
    }

}

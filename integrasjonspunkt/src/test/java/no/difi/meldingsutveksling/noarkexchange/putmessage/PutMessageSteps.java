package no.difi.meldingsutveksling.noarkexchange.putmessage;

import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.IntegrasjonspunktImpl;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.noarkexchange.schema.*;
import no.difi.meldingsutveksling.services.AdresseregisterService;


import org.mockito.Mockito;

import static junit.framework.TestCase.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

/**
 * Makes sure that the integrasjonspunkt can handle receipt messages on
 * the putMessage interface
 * @author Glenn Bech
 */
public class PutMessageSteps {

    private String appReceiptPayload = "&lt;AppReceipt type=\"OK\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.arkivverket.no/Noark/Exchange/types\"&gt;\n" +
            "    &lt;message code=\"ID\" xmlns=\"\"&gt;\n" +
            "    &lt;text&gt;210725&lt;/text&gt;\n" +
            "    &lt;/message&gt;\n" +
            "    &lt;/AppReceipt&gt;";

    private IntegrasjonspunktImpl integrasjonspunkt;
    private AdresseregisterService adresseregister ;
    private EventLog eventLog ;
    private PutMessageRequestType message;
    private MessageSender messageSender;

    @Before
    public void setup() {
        integrasjonspunkt = new IntegrasjonspunktImpl();
        adresseregister = mock(AdresseregisterService.class);
        eventLog = mock(EventLog.class);
        integrasjonspunkt.setEventLog(eventLog);

        messageSender = mock(MessageSender.class);
        messageSender.setAdresseregister(adresseregister);
        messageSender.setEventLog(eventLog);
        integrasjonspunkt.setMessageSender(messageSender);
  }

    @Given("^en kvittering$")
    public void et_dokument_mottatt_på_putmessage_grensesnittet() {

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
        message.setPayload(appReceiptPayload);
    }

    @When("^integrasjonspunktet mottar en kvittering på putMessage grensesnittet$")
    public void integrasjonspunkt_mottar_kvittering(){
        integrasjonspunkt.putMessage(message);
    }

    @Then("^kvitteringen logges i integrasjonspunktet sin hendelseslogg$")
    public void kvitteringen_logges_i_eventlog()  {
        verify(eventLog).log(any(Event.class));
    }

    @Then("^kvitteringen sendes ikke videre til transport$")
    public void kvitteringen_sendes_ikke_videre()  {
        verify(messageSender, never()).sendMessage(any(PutMessageRequestType.class));
    }
}
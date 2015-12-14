package no.difi.meldingsutveksling.noarkexchange.putmessage;

import cucumber.api.PendingException;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import no.difi.asic.SignatureHelper;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.sbdh.BusinessScope;
import no.difi.meldingsutveksling.domain.sbdh.Document;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.IntegrasjonspunktImpl;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.noarkexchange.StandardBusinessDocumentFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.EnvelopeType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.services.AdresseregisterVirksert;
import no.difi.meldingsutveksling.services.CertificateException;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.apache.commons.configuration.Configuration;
import sun.security.x509.X509CertImpl;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private AdresseregisterVirksert adresseregister ;
    private EventLog eventLog ;
    private PutMessageRequestType message;
    private MessageSender messageSender;
    private Transport transport;
    private IntegrasjonspunktNokkel integrasjonspunktNokkel;


    @Before
    public void setup() throws MessageException, CertificateException {
        integrasjonspunkt = new IntegrasjonspunktImpl();
        adresseregister = mock(AdresseregisterVirksert.class);
        when(adresseregister.getCertificate(any(String.class))).thenReturn(new X509CertImpl());
        eventLog = mock(EventLog.class);
        integrasjonspunkt.setEventLog(eventLog);

        messageSender = new MessageSender();
        messageSender.setAdresseregister(adresseregister);
        messageSender.setEventLog(eventLog);
        integrasjonspunktNokkel = mock(IntegrasjonspunktNokkel.class);
        when(integrasjonspunktNokkel.getSignatureHelper()).thenReturn(mock(SignatureHelper.class));
        StandardBusinessDocumentFactory documentFactory = mock(StandardBusinessDocumentFactory.class);

        Document document = createStandardBusinessDocument();

        when(documentFactory.create(any(PutMessageRequestType.class), any(no.difi.meldingsutveksling.domain.Avsender.class), any(Mottaker.class))).thenReturn(document);
        messageSender.setStandardBusinessDocumentFactory(documentFactory);
        messageSender.setKeyInfo(integrasjonspunktNokkel);
        messageSender.setConfiguration(mock(IntegrasjonspunktConfig.class));

        TransportFactory transportFactory = mock(TransportFactory.class);
        transport = mock(Transport.class);
        when(transportFactory.createTransport(any(Document.class))).thenReturn(transport);
        messageSender.setTransportFactory(transportFactory);

        integrasjonspunkt.setMessageSender(messageSender);
        integrasjonspunkt.setAdresseRegister(adresseregister);
  }

    private Document createStandardBusinessDocument() {
        Document document = new Document();
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader();
        BusinessScope scope = new BusinessScope();
        ArrayList<Scope> scopes = new ArrayList<>();
        scopes.add(new Scope());
        scope.setScope(scopes);
        header.setBusinessScope(scope);
        document.setStandardBusinessDocumentHeader(header);
        return document;
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
        verify(transport, never()).send(any(Configuration.class), any(Document.class));
    }

    @Given("^en velformet melding fra (.+)$")
    public void en_velformet_melding_fra_arkivsystem(String arkivSystem) throws Throwable {
        JAXBContext jaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        Path testDataPath = Paths.get("/testdata", String.format("%s.xml", arkivSystem));

        InputStream stream = this.getClass().getResourceAsStream(testDataPath.toString());
        if(stream != null) {
            message = unmarshaller.unmarshal(new StreamSource(stream), PutMessageRequestType.class).getValue();
        } else {
            throw new PendingException(String.format("missing test data for %s. Add a request in location %s", arkivSystem, testDataPath.toAbsolutePath().toString()));
        }
    }

    @Then("^skal melding bli videresendt$")
    public void skal_melding_bli_videresendt() throws Throwable {
        verify(transport).send(any(Configuration.class), any(Document.class));
    }

    @When("^integrasjonspunktet mottar meldingen$")
    public void integrasjonspunktet_mottar_meldingen() throws Throwable {
        integrasjonspunkt.putMessage(message);
    }
}
package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.EnvelopeType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.SOAPport;
import org.springframework.boot.context.embedded.LocalServerPort;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.IOException;
import java.net.URL;

public class EduSteps {

    private final Holder<Message> messageOutHolder;
    private final int port;

    public EduSteps(
            Holder<Message> messageOutHolder,
            @LocalServerPort int port) {
        this.messageOutHolder = messageOutHolder;
        this.port = port;
    }

    @Before
    public void before() {
        // NOOP
    }

    @Given("^the BEST/EDU payload is:$")
    public void theBESTEDUPayloadIs(String payload) {
        messageOutHolder.get().setBody(payload);
    }

    @Given("^I call the Noark.Exchange WebService$")
    public void iCallTheNoarkExchangeWebService() throws IOException {
        // Data to access the web service
        URL wsdlDocumentLocation = new URL(String.format("http://localhost:%d/noarkExchange?wsdl", port));
        String namespaceURI = "http://www.arkivverket.no/Noark/Exchange";
        String servicePart = "noarkExchange";
        String portName = "NoarkExchangePort";
        QName serviceQN = new QName(namespaceURI, servicePart);
        QName portQN = new QName(namespaceURI, portName);

        // Creates a service instance
        Service service = Service.create(wsdlDocumentLocation, serviceQN);
        SOAPport soapPort = service.getPort(portQN, SOAPport.class);

        soapPort.putMessage(getMessageRequestType());
    }

    private PutMessageRequestType getMessageRequestType() {
        Message message = messageOutHolder.get();

        PutMessageRequestType request = new PutMessageRequestType();
        EnvelopeType envelope = new EnvelopeType();
        AddressType senderAddress = new AddressType();
        senderAddress.setOrgnr(message.getSender());
        envelope.setSender(senderAddress);
        envelope.setConversationId(message.getConversationId());
        AddressType receiverAddress = new AddressType();
        receiverAddress.setOrgnr(message.getReceiver());
        envelope.setReceiver(receiverAddress);
        request.setEnvelope(envelope);
        request.setPayload(message.getBody());
        return request;
    }
}

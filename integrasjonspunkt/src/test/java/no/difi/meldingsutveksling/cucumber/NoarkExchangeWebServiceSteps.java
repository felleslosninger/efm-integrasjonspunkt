package no.difi.meldingsutveksling.cucumber;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import no.difi.meldingsutveksling.noarkexchange.schema.*;
import org.springframework.boot.web.server.LocalServerPort;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class NoarkExchangeWebServiceSteps {

    private final Holder<Message> messageOutHolder;
    private final int port;

    public NoarkExchangeWebServiceSteps(
            Holder<Message> messageOutHolder,
            @LocalServerPort int port) {
        this.messageOutHolder = messageOutHolder;
        this.port = port;
    }

    @Before
    public void before() {
        // NOOP
    }

    @Given("^I call the noarkExchange WebService$")
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

        PutMessageResponseType putMessageResponseType = soapPort.putMessage(getMessageRequestType());
        assertThat(putMessageResponseType.getResult().getType()).isEqualTo("OK");
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

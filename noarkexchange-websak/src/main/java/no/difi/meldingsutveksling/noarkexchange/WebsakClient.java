package no.difi.meldingsutveksling.noarkexchange;


import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.websak.PutMessageRequestMapper;
import no.difi.meldingsutveksling.noarkexchange.websak.schema.*;
import org.modelmapper.ModelMapper;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

import static no.difi.meldingsutveksling.logging.MarkerFactory.receiverMarker;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageMarker.markerFrom;

public class WebsakClient implements NoarkClient {

    private static final String SOAP_ACTION = "http://www.arkivverket.no/Noark/Exchange/IEDUImport/PutMessage";
    private final WebServiceTemplateFactory templateFactory;
    private NoarkClientSettings settings;

    public WebsakClient(NoarkClientSettings settings) {
        this.settings = settings;
        this.templateFactory = settings.createTemplateFactory();
    }

    @Override
    public NoarkClientSettings getNoarkClientSettings() {
        return settings;
    }

    @Override
    public boolean canRecieveMessage(String orgnr) {
        GetCanReceiveMessageRequestType r = new GetCanReceiveMessageRequestType();
        AddressType addressType = new AddressType();
        addressType.setOrgnr(orgnr);
        r.setReceiver(addressType);

        JAXBElement<GetCanReceiveMessageRequestType> request = new ObjectFactory().createGetCanReceiveMessageRequest(r);

        final WebServiceTemplate template = templateFactory.createTemplate("no.difi.meldingsutveksling.noarkexchange.websak.schema", receiverMarker(orgnr));
        JAXBElement<GetCanReceiveMessageResponseType> result = (JAXBElement<GetCanReceiveMessageResponseType>) template.marshalSendAndReceive(settings.getEndpointUrl(), request);
        return result.getValue().isResult();
    }

    @Override
    public PutMessageResponseType sendEduMelding(PutMessageRequestType request) {
        JAXBElement<no.difi.meldingsutveksling.noarkexchange.websak.schema.PutMessageRequestType> websakRequest
                = new PutMessageRequestMapper().mapFrom(request);

        final WebServiceTemplate template = templateFactory.createTemplate("no.difi.meldingsutveksling.noarkexchange.websak.schema", markerFrom(new PutMessageRequestWrapper(request)));
        JAXBElement<no.difi.meldingsutveksling.noarkexchange.websak.schema.PutMessageResponseType> response
                = (JAXBElement) template.marshalSendAndReceive(settings.getEndpointUrl(), websakRequest,
                new SoapActionCallback(SOAP_ACTION));

        PutMessageResponseType theResponse = new PutMessageResponseType();
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.map(response.getValue(), theResponse);


        setUnmappedValues(response, theResponse);

        return theResponse;
    }

    /**
     * Use this method to set values not "mapped" by modelmapper. For instance statusMessage
     *
     * @param websakResponse from the archive system
     * @param response       websakResponse from this client
     */
    private void setUnmappedValues(JAXBElement<no.difi.meldingsutveksling.noarkexchange.websak.schema.PutMessageResponseType> websakResponse, PutMessageResponseType response) {
        List<StatusMessageType> statusMessages = getStatusMessages(websakResponse);

        if (!statusMessages.isEmpty()) {
            no.difi.meldingsutveksling.noarkexchange.schema.StatusMessageType statusMessage = new no.difi.meldingsutveksling.noarkexchange.schema.StatusMessageType();
            statusMessage.setCode(statusMessages.get(0).getCode());
            statusMessage.setText(statusMessages.get(0).getText());
            response.getResult().getMessage().add(statusMessage);
        }
    }

    private List<StatusMessageType> getStatusMessages(JAXBElement<no.difi.meldingsutveksling.noarkexchange.websak.schema.PutMessageResponseType> response) {
        List<StatusMessageType> statusMessageTypes = new ArrayList<>();

        if (response.isNil()) {
            return statusMessageTypes;
        }

        AppReceiptType appReceipt = response.getValue().getResult();
        if (appReceipt != null) {
            statusMessageTypes = appReceipt.getMessage();
        }

        return statusMessageTypes;
    }
}

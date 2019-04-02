package no.difi.meldingsutveksling.noarkexchange;


import no.difi.meldingsutveksling.noarkexchange.p360.PutMessageRequestMapper;
import no.difi.meldingsutveksling.noarkexchange.p360.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.p360.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.p360.schema.GetCanReceiveMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.p360.schema.GetCanReceiveMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.p360.schema.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.p360.schema.StatusMessageType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import org.modelmapper.ModelMapper;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.List;

import static no.difi.meldingsutveksling.logging.MarkerFactory.receiverMarker;

public class P360Client implements NoarkClient {

    private static final String SOAP_ACTION = "http://www.arkivverket.no/Noark/Exchange/IEDUImport/PutMessage";

    private final WebServiceTemplateFactory templateFactory;
    private final NoarkClientSettings settings;

    public P360Client(NoarkClientSettings settings) {
        this.settings = settings;
        templateFactory = settings.createTemplateFactory();
    }

    @Override
    public boolean canRecieveMessage(String orgnr) {
        GetCanReceiveMessageRequestType r = new GetCanReceiveMessageRequestType();
        AddressType addressType = new AddressType();
        addressType.setOrgnr(orgnr);
        r.setReceiver(addressType);

        JAXBElement<GetCanReceiveMessageRequestType> request = new ObjectFactory().createGetCanReceiveMessageRequest(r);

        final WebServiceTemplate template = templateFactory.createTemplate("no.difi.meldingsutveksling.noarkexchange.p360.schema", receiverMarker(orgnr));
        JAXBElement<GetCanReceiveMessageResponseType> result = (JAXBElement<GetCanReceiveMessageResponseType>) template.marshalSendAndReceive(settings.getEndpointUrl(), request);
        return result.getValue().isResult();
    }

    @Override
    public PutMessageResponseType sendEduMelding(PutMessageRequestType request) {

        JAXBElement<no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType> p360request;
        try {
            p360request = new PutMessageRequestMapper().mapFrom(request);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create PutMessageRequest for P360", e);
        }

        final WebServiceTemplate template = templateFactory.createTemplate("no.difi.meldingsutveksling.noarkexchange.p360.schema", PutMessageMarker.markerFrom(new PutMessageRequestWrapper(request)));
        JAXBElement<no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageResponseType> response
                = (JAXBElement) template.marshalSendAndReceive(settings.getEndpointUrl(), p360request,
                new SoapActionCallback(SOAP_ACTION));

        PutMessageResponseType theResponse = new PutMessageResponseType();
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.map(response.getValue(), theResponse);



        setUnmappedValues(response, theResponse);

        if (!isValid(theResponse)) {
            theResponse = new PutMessageResponseType();
            final no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType appReceiptType = new no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType();
            theResponse.setResult(appReceiptType);
            appReceiptType.setType("OK");
        }

        return theResponse;
    }

    private boolean isValid(PutMessageResponseType theResponse) {
        return theResponse != null && theResponse.getResult() != null;
    }

    /**
     * Use this method to set values not "mapped" by modelmapper. For instance statusMessage
     * @param p360Response from the archive system
     * @param response p360Response from this client
     */
    private void setUnmappedValues(JAXBElement<no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageResponseType> p360Response, PutMessageResponseType response) {
        List<StatusMessageType> statusMessages = getStatusMessages(p360Response);

        if(!statusMessages.isEmpty()) {
            no.difi.meldingsutveksling.noarkexchange.schema.StatusMessageType statusMessage = new no.difi.meldingsutveksling.noarkexchange.schema.StatusMessageType();
            statusMessage.setCode(statusMessages.get(0).getCode());
            statusMessage.setText(statusMessages.get(0).getText());
            response.getResult().getMessage().add(statusMessage);
        }
    }

    private List<StatusMessageType> getStatusMessages(JAXBElement<no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageResponseType> response){
        List<StatusMessageType> statusMessageTypes = new ArrayList<>() ;

        if(response.isNil()){
            return statusMessageTypes;
        }

        AppReceiptType appReceipt = response.getValue().getResult();
        if(appReceipt != null){
            statusMessageTypes = appReceipt.getMessage();
        }

        return statusMessageTypes;
    }
}

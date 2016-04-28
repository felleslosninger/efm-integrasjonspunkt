package no.difi.meldingsutveksling.noarkexchange;


import no.difi.meldingsutveksling.noarkexchange.p360.schema.*;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import org.modelmapper.ModelMapper;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

public class P360Client implements NoarkClient {

    public static final String SOAP_ACTION = "http://www.arkivverket.no/Noark/Exchange/IEDUImport/PutMessage";
    private final WebServiceTemplate template;
    private NoarkClientSettings settings;

    public P360Client(NoarkClientSettings settings, WebServiceTemplateFactory templateFactory) {
        this.settings = settings;
        this.template = templateFactory.createTemplate("no.difi.meldingsutveksling.noarkexchange.p360.schema");
    }

    @Override
    public boolean canRecieveMessage(String orgnr) {
        GetCanReceiveMessageRequestType r = new GetCanReceiveMessageRequestType();
        AddressType addressType = new AddressType();
        addressType.setOrgnr(orgnr);
        r.setReceiver(addressType);

        JAXBElement<GetCanReceiveMessageRequestType> request = new ObjectFactory().createGetCanReceiveMessageRequest(r);

        JAXBElement<GetCanReceiveMessageResponseType> result = (JAXBElement<GetCanReceiveMessageResponseType>) template.marshalSendAndReceive(settings.getEndpointUrl(), request);
        return result.getValue().isResult();
    }

    @Override
    public PutMessageResponseType sendEduMelding(PutMessageRequestType request) {
        no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType r =
                new no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType();

        ModelMapper mapper = new ModelMapper();
        mapper.map(request, r);

        JAXBElement<no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType> p360request
                = new no.difi.meldingsutveksling.noarkexchange.p360.schema.ObjectFactory().createPutMessageRequest(r);


        JAXBElement<no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageResponseType> response
                = (JAXBElement) template.marshalSendAndReceive(settings.getEndpointUrl(), p360request,
                new SoapActionCallback(SOAP_ACTION));

        PutMessageResponseType theResponse = new PutMessageResponseType();
        mapper.map(response.getValue(), theResponse);


        setUnmappedValues(response, theResponse);

        return theResponse;
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

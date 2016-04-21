package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.ephorte.schema.*;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import org.modelmapper.ModelMapper;
import org.springframework.ws.client.core.WebServiceTemplate;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

public class EphorteClient implements NoarkClient {
    private final WebServiceTemplate template;
    private final NoarkClientSettings settings;

    public EphorteClient(NoarkClientSettings settings, WebServiceTemplateFactory templateFactory) {
        this.template = templateFactory.createTemplate("no.difi.meldingsutveksling.noarkexchange.ephorte.schema");
        this.settings = settings;
    }

    @Override
    public boolean canRecieveMessage(String orgnr) {
        GetCanReceiveMessageRequestType r = new GetCanReceiveMessageRequestType();
        AddressType addressType = new AddressType();
        addressType.setOrgnr(orgnr);
        r.setReceiver(addressType);
        JAXBElement<GetCanReceiveMessageRequestType> ephorteRequest = new ObjectFactory().createGetCanReceiveMessageRequest(r);

        JAXBElement<GetCanReceiveMessageResponseType> result = (JAXBElement<GetCanReceiveMessageResponseType>) template.marshalSendAndReceive(settings.getEndpointUrl(), ephorteRequest);
        return result.getValue().isResult();
    }

    @Override
    public PutMessageResponseType sendEduMelding(PutMessageRequestType request) {
        ModelMapper modelMapper = new ModelMapper();
        no.difi.meldingsutveksling.noarkexchange.ephorte.schema.PutMessageRequestType r = new no.difi.meldingsutveksling.noarkexchange.ephorte.schema.PutMessageRequestType();
        modelMapper.map(request, r);
        JAXBElement<no.difi.meldingsutveksling.noarkexchange.ephorte.schema.PutMessageRequestType> ephorteRequest = new ObjectFactory().createPutMessageRequest(r);
        JAXBElement<no.difi.meldingsutveksling.noarkexchange.ephorte.schema.PutMessageResponseType>
                ephorteResponse = (JAXBElement<no.difi.meldingsutveksling.noarkexchange.ephorte.schema.PutMessageResponseType>)
                template.marshalSendAndReceive(settings.getEndpointUrl(), ephorteRequest);

        PutMessageResponseType response = new PutMessageResponseType();
        modelMapper.map(ephorteResponse.getValue(), response);

        setUnmappedValues(ephorteResponse, response);

        return response;
    }

    /**
     * Use this method to set values not "mapped" by modelmapper. For instance statusMessage
     * @param ephorteResponse response from the archive system
     * @param response response from this client
     */
    private void setUnmappedValues(JAXBElement<no.difi.meldingsutveksling.noarkexchange.ephorte.schema.PutMessageResponseType> ephorteResponse, PutMessageResponseType response) {
        List<StatusMessageType> statusMessages = getStatusMessages(ephorteResponse);

        if(!statusMessages.isEmpty()) {
            no.difi.meldingsutveksling.noarkexchange.schema.StatusMessageType statusMessage = new no.difi.meldingsutveksling.noarkexchange.schema.StatusMessageType();
            statusMessage.setCode(statusMessages.get(0).getCode());
            statusMessage.setText(statusMessages.get(0).getText());
            response.getResult().getMessage().add(statusMessage);
        }
    }

    private List<StatusMessageType> getStatusMessages(JAXBElement<no.difi.meldingsutveksling.noarkexchange.ephorte.schema.PutMessageResponseType> response){
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

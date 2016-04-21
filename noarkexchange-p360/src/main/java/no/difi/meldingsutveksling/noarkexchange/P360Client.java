package no.difi.meldingsutveksling.noarkexchange;


import no.difi.meldingsutveksling.noarkexchange.p360.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.p360.schema.GetCanReceiveMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.p360.schema.GetCanReceiveMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.p360.schema.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import org.modelmapper.ModelMapper;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;

import javax.xml.bind.JAXBElement;
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

        List<no.difi.meldingsutveksling.noarkexchange.p360.schema.StatusMessageType> statusMessages = response.getValue().getResult().getMessage();

        if(!statusMessages.isEmpty()) {
            no.difi.meldingsutveksling.noarkexchange.schema.StatusMessageType statusMesage = new no.difi.meldingsutveksling.noarkexchange.schema.StatusMessageType();
            statusMesage.setCode(statusMessages.get(0).getCode());
            statusMesage.setText(statusMessages.get(0).getText());
            theResponse.getResult().getMessage().add(statusMesage);
        }

        return theResponse;
    }
}

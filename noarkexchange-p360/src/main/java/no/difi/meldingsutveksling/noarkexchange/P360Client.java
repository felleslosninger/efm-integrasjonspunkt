package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import org.modelmapper.ModelMapper;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;

import javax.xml.bind.JAXBElement;

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
        return false;
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
                = (JAXBElement) template.marshalSendAndReceive(settings.getEndpointUrl(), request,
                new SoapActionCallback(SOAP_ACTION));

        PutMessageResponseType theResponse = new PutMessageResponseType();
        mapper.map(response.getValue(), theResponse);
        return theResponse;
    }
}

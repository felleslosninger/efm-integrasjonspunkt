package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.*;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;

import javax.xml.bind.JAXBElement;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.logging.MarkerFactory.receiverMarker;

public class MshClient implements NoarkClient {

    private static final String SOAP_ACTION = "http://www.arkivverket.no/Noark/Exchange/IEDUImport/PutMessage";

    private String endpointURL;
    private WebServiceTemplateFactory templateFactory;


    public MshClient(String endpointURL) {
        this.endpointURL = endpointURL;
        this.templateFactory = new DefaultTemplateFactory();
    }

    @Override
    public boolean canRecieveMessage(String orgnr) {

        if (isNullOrEmpty(endpointURL)) {
            return false;
        }

        GetCanReceiveMessageRequestType requestType = new GetCanReceiveMessageRequestType();
        AddressType addressType = new AddressType();
        addressType.setOrgnr(orgnr);
        requestType.setReceiver(addressType);

        JAXBElement<GetCanReceiveMessageRequestType> request = new ObjectFactory().createGetCanReceiveMessageRequest(requestType);
        final WebServiceTemplate template = templateFactory.createTemplate("no.difi.meldingsutveksling.noarkexchange.schema", receiverMarker(orgnr));
        @SuppressWarnings("unchecked")
        JAXBElement<GetCanReceiveMessageResponseType> result = (JAXBElement) template.marshalSendAndReceive(endpointURL, request);
        return result.getValue().isResult();
    }

    @Override
    public PutMessageResponseType sendEduMelding(PutMessageRequestType request) {

        final WebServiceTemplate template = templateFactory.createTemplate("no.difi.meldingsutveksling.noarkexchange.schema",
                PutMessageMarker.markerFrom(new PutMessageRequestWrapper(request)));
        ObjectFactory of = new ObjectFactory();
        JAXBElement<PutMessageRequestType> putMessageRequest = of.createPutMessageRequest(request);
        @SuppressWarnings("unchecked")
        JAXBElement<PutMessageResponseType> response = (JAXBElement) template.marshalSendAndReceive(endpointURL,
                putMessageRequest,
                new SoapActionCallback(SOAP_ACTION));

        PutMessageResponseType responseValue = response.getValue();

        if (responseValue == null || responseValue.getResult() == null) {
            responseValue = new PutMessageResponseType();
            final AppReceiptType appReceiptType = new AppReceiptType();
            appReceiptType.setType("OK");
            responseValue.setResult(appReceiptType);
        }

        return responseValue;
    }
}

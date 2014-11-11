package no.difi.meldingsutveksling.noarkexchange.schema;

import no.difi.meldingsutveksling.noarkexchange.SendMessageTemplate;
import no.difi.meldingsutveksling.noarkexchange.OxalisSendMessageTemplate;

import javax.jws.WebService;
import javax.xml.ws.BindingType;

/**
 * This is the implementation of the wenbservice that case managenent systems supporting
 * the BEST/EDU stadard communicates with. The responsibility of this component is to
 * create, sign and encrypt a SBD message for delivery to a PEPPOL access point
 * <p/>
 * The access point for the recipient is looked up through ELMA and SMK, the certificates are
 * retrived through a MOCKED adress register component not yet imolemented in any infrastructure.
 * <p/>
 * <p/>
 * User: glennbech
 * Date: 31.10.14
 * Time: 15:26
 */


@WebService(portName = "NoarkExchangePort", serviceName = "noarkExchange", targetNamespace = "http://www.arkivverket.no/Noark/Exchange", wsdlLocation = "http://hardcodeme.not", endpointInterface = "no.difi.meldingsutveksling.noarkexchange.schema.SOAPport")
@BindingType("http://schemas.xmlsoap.org/wsdl/soap/http")
public class KnutepunkImpl extends noarkExchange_NoarkExchangePortImpl {

    @Override
    public GetCanReceiveMessageResponseType getCanReceiveMessage(GetCanReceiveMessageRequestType getCanReceiveMessageRequest) {
        return super.getCanReceiveMessage(getCanReceiveMessageRequest);
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType putMessageRequest) {
        SendMessageTemplate template = new OxalisSendMessageTemplate();
        return template.sendMessage(putMessageRequest);
    }

}

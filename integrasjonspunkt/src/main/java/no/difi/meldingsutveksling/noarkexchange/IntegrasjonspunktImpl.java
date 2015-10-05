package no.difi.meldingsutveksling.noarkexchange;

import com.thoughtworks.xstream.XStream;
import no.difi.meldingsutveksling.domain.ProcessState;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.schema.*;
import no.difi.meldingsutveksling.services.AdresseregisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.BindingType;

import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.*;

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

@Component("noarkExchangeService")
@WebService(portName = "NoarkExchangePort", serviceName = "noarkExchange", targetNamespace = "http://www.arkivverket.no/Noark/Exchange", endpointInterface = "no.difi.meldingsutveksling.noarkexchange.schema.SOAPport")
@BindingType("http://schemas.xmlsoap.org/wsdl/soap/http")
public class IntegrasjonspunktImpl implements SOAPport {

    @Autowired
    AdresseregisterService adresseregister;

    @Autowired
    private MessageSender messageSender;

    @Autowired
    private EventLog eventLog;

    @Override
    public GetCanReceiveMessageResponseType getCanReceiveMessage(@WebParam(name = "GetCanReceiveMessageRequest", targetNamespace = "http://www.arkivverket.no/Noark/Exchange/types", partName = "getCanReceiveMessageRequest") GetCanReceiveMessageRequestType getCanReceiveMessageRequest) {

        String organisasjonsnummer = getCanReceiveMessageRequest.getReceiver().getOrgnr();

        eventLog.log(new Event()
                .setMessage(new XStream().toXML(getCanReceiveMessageRequest))
                .setProcessStates(ProcessState.CAN_RECEIVE_INVOKED)
                .setReceiver(getCanReceiveMessageRequest.getReceiver().getOrgnr()).setSender("NA"));

        GetCanReceiveMessageResponseType response = new GetCanReceiveMessageResponseType();
        boolean canReceive = true;
        try {
            adresseregister.getCertificate(organisasjonsnummer);
        } catch (Exception e) {
            canReceive = false;
        }
        response.setResult((canReceive));
        return response;
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType putMessageRequest) {
        Object payload = putMessageRequest.getPayload();
        if (payload instanceof AppReceiptType) {
            AppReceiptType receipt = (AppReceiptType) payload;
            for (StatusMessageType sm : receipt.getMessage())
                eventLog.log(new Event(ProcessState.APP_RECEIPT).setMessage(sm.getCode() + ", " + sm.getText()));
            return createOkResponse();
        } else {
            return messageSender.sendMessage(putMessageRequest);
        }
    }

    public AdresseregisterService getAdresseRegisterClient() {
        return adresseregister;
    }

    public void setAdresseRegisterClient(AdresseregisterService adresseRegisterClient) {
        this.adresseregister = adresseRegisterClient;
    }

    public MessageSender getMessageSender() {
        return messageSender;
    }

    public void setMessageSender(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    public EventLog getEventLog() {
        return eventLog;
    }

    public void setEventLog(EventLog eventLog) {
        this.eventLog = eventLog;
    }
}

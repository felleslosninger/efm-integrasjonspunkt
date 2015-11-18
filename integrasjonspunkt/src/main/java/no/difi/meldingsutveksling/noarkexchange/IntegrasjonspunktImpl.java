package no.difi.meldingsutveksling.noarkexchange;

import com.thoughtworks.xstream.XStream;
import no.difi.meldingsutveksling.domain.ProcessState;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.SOAPport;
import no.difi.meldingsutveksling.queue.service.QueueService;
import no.difi.meldingsutveksling.services.AdresseregisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jws.WebParam;
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

    @Autowired
    private NoarkClient mshClient;

    @Autowired
    private QueueService queueService;

    @Override
    public GetCanReceiveMessageResponseType getCanReceiveMessage(@WebParam(name = "GetCanReceiveMessageRequest", targetNamespace = "http://www.arkivverket.no/Noark/Exchange/types", partName = "getCanReceiveMessageRequest") GetCanReceiveMessageRequestType getCanReceiveMessageRequest) {

        String organisasjonsnummer = getCanReceiveMessageRequest.getReceiver().getOrgnr();
        eventLog.log(new Event()
                .setMessage(new XStream().toXML(getCanReceiveMessageRequest))
                .setProcessStates(ProcessState.CAN_RECEIVE_INVOKED)
                .setReceiver(getCanReceiveMessageRequest.getReceiver().getOrgnr()).setSender("NA"));

        GetCanReceiveMessageResponseType response = new GetCanReceiveMessageResponseType();
        boolean canReceive;
        if (hasAdresseregisterCertificate(organisasjonsnummer)) {
            canReceive = true;
        } else {
            canReceive = mshClient.canRecieveMessage(organisasjonsnummer);
        }
        response.setResult(canReceive);
        return response;
    }

    private boolean hasAdresseregisterCertificate(String organisasjonsnummer) {
        try {
            adresseregister.getCertificate(organisasjonsnummer);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType request) {
        queueService.put(request);

        return PutMessageResponseFactory.createOkResponse();

        //        if(hasAdresseregisterCertificate(req.getEnvelope().getReceiver().getOrgnr())) {
        //            PutMessageContext context = new PutMessageContext(eventLog, messageSender);
        //            PutMessageStrategyFactory putMessageStrategyFactory = PutMessageStrategyFactory.newInstance(context);
        //
        //            PutMessageStrategy strategy = putMessageStrategyFactory.create(req.getPayload());
        //            return strategy.putMessage(req);
        //        } else {
        //            return mshClient.sendEduMelding(req);
        //        }
    }

    public AdresseregisterService getAdresseRegister() {
        return adresseregister;
    }

    public void setAdresseRegister(AdresseregisterService adresseRegisterClient) {
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

    public void setMshClient(NoarkClient mshClient) {
        this.mshClient = mshClient;
    }

    public NoarkClient getMshClient() {
        return mshClient;
    }
}

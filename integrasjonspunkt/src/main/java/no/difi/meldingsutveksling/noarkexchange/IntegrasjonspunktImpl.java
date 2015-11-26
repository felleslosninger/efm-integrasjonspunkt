package no.difi.meldingsutveksling.noarkexchange;

import com.thoughtworks.xstream.XStream;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.domain.ProcessState;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.putmessage.PutMessageContext;
import no.difi.meldingsutveksling.noarkexchange.putmessage.PutMessageStrategy;
import no.difi.meldingsutveksling.noarkexchange.putmessage.PutMessageStrategyFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.*;
import no.difi.meldingsutveksling.queue.service.QueueService;
import no.difi.meldingsutveksling.services.AdresseregisterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.BindingType;
import java.io.IOException;

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

@org.springframework.stereotype.Component("noarkExchangeService")
@WebService(portName = "NoarkExchangePort", serviceName = "noarkExchange", targetNamespace = "http://www.arkivverket.no/Noark/Exchange", endpointInterface = "no.difi.meldingsutveksling.noarkexchange.schema.SOAPport")
@BindingType("http://schemas.xmlsoap.org/wsdl/soap/http")
public class IntegrasjonspunktImpl implements SOAPport {
    private static final Logger log = LoggerFactory.getLogger(IntegrasjonspunktImpl.class);

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

    @Autowired
    IntegrasjonspunktConfig integrasjonspunktConfig;

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
        if (integrasjonspunktConfig.isQueueEnabled()) {
            try {
                queueService.put(request);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
            return PutMessageResponseFactory.createOkResponse();
        }
        else {
            if (hasAdresseregisterCertificate(request.getEnvelope().getReceiver().getOrgnr())) {
                PutMessageContext context = new PutMessageContext(eventLog, messageSender);
                PutMessageStrategyFactory putMessageStrategyFactory = PutMessageStrategyFactory.newInstance(context);

                PutMessageStrategy strategy = putMessageStrategyFactory.create(request.getPayload());
                return strategy.putMessage(request);
            } else {
                return mshClient.sendEduMelding(request);
            }
        }
    }

    public boolean sendMessage(PutMessageRequestType request) {
        if (hasAdresseregisterCertificate(request.getEnvelope().getReceiver().getOrgnr())) {
            PutMessageContext context = new PutMessageContext(eventLog, messageSender);
            PutMessageStrategyFactory putMessageStrategyFactory = PutMessageStrategyFactory.newInstance(context);

            PutMessageStrategy strategy = putMessageStrategyFactory.create(request.getPayload());
            strategy.putMessage(request);
        } else {
            mshClient.sendEduMelding(request);
        }
        return true;
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

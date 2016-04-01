package no.difi.meldingsutveksling.noarkexchange;

import com.thoughtworks.xstream.XStream;
import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.ProcessState;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.logging.MessageMarkerFactory;
import no.difi.meldingsutveksling.noarkexchange.putmessage.PutMessageContext;
import no.difi.meldingsutveksling.noarkexchange.putmessage.PutMessageStrategy;
import no.difi.meldingsutveksling.noarkexchange.putmessage.PutMessageStrategyFactory;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.SOAPport;
import no.difi.meldingsutveksling.services.AdresseregisterVirksert;
import no.difi.meldingsutveksling.services.CertificateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.BindingType;

import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;

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
    AdresseregisterVirksert adresseregister;

    @Autowired
    private MessageSender messageSender;

    @Autowired
    private EventLog eventLog;

    @Autowired
    private NoarkClient mshClient;

    @Autowired
    private InternalQueue internalQueue;

    @Autowired
    private IntegrasjonspunktConfiguration configuration;

    @Override
    public GetCanReceiveMessageResponseType getCanReceiveMessage(@WebParam(name = "GetCanReceiveMessageRequest", targetNamespace = "http://www.arkivverket.no/Noark/Exchange/types", partName = "getCanReceiveMessageRequest") GetCanReceiveMessageRequestType getCanReceiveMessageRequest) {


        String organisasjonsnummer = getCanReceiveMessageRequest.getReceiver().getOrgnr();
        eventLog.log(new Event()
                .setMessage(new XStream().toXML(getCanReceiveMessageRequest))
                .setProcessStates(ProcessState.CAN_RECEIVE_INVOKED)
                .setReceiver(getCanReceiveMessageRequest.getReceiver().getOrgnr()).setSender("NA"));

        GetCanReceiveMessageResponseType response = new GetCanReceiveMessageResponseType();
        boolean canReceive;

        canReceive = hasAdresseregisterCertificate(organisasjonsnummer);

        final LogstashMarker marker = MessageMarkerFactory.receiverMarker(organisasjonsnummer);
        if(canReceive) {
            Audit.info("Mottaker kan motta meldinger", marker);
        } else {
            Audit.info("Mangler mottakers sertifikat, sjekker om MSH kan motta meldinger", marker);
            canReceive = mshClient.canRecieveMessage(organisasjonsnummer);
        }
        if(!canReceive) {
            Audit.error("Mottaker kan ikke motta meldinger", marker);
        }
        response.setResult(canReceive);
        return response;
    }

    private boolean hasAdresseregisterCertificate(String organisasjonsnummer) {
        try {
            String nOrgnr = FiksFix.replaceOrgNummberWithKs(organisasjonsnummer);
            adresseregister.getCertificate(nOrgnr);
            return true;
        } catch (CertificateException e) {
            return false;
        }
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType request) {
        MDC.put(IntegrasjonspunktConfiguration.KEY_ORGANISATION_NUMBER, configuration.getOrganisationNumber());
        PutMessageRequestWrapper message = new PutMessageRequestWrapper(request);
        if (!message.hasSenderPartyNumber()) {
            message.setSenderPartyNumber(configuration.getOrganisationNumber());
        }
        Audit.info("Recieved message", markerFrom(message));
        if (!message.hasSenderPartyNumber() && !configuration.hasOrganisationNumber()) {
            throw new MeldingsUtvekslingRuntimeException("Missing senders orgnumber. Please configure orgnumber= in the integrasjonspunkt-local.properties");
        }

        if (configuration.isQueueEnabled()) {
            internalQueue.enqueueExternal(request);
            Audit.info("Message is put on queue ready to be sent", markerFrom(message));

            return PutMessageResponseFactory.createOkResponse();
        }
        else {
            Audit.info("Queue is disabled. Message will be sent immediatly", markerFrom(message));

            if (hasAdresseregisterCertificate(request.getEnvelope().getReceiver().getOrgnr())) {
                PutMessageContext context = new PutMessageContext(eventLog, messageSender);
                PutMessageStrategyFactory putMessageStrategyFactory = PutMessageStrategyFactory.newInstance(context);
                PutMessageStrategy strategy = putMessageStrategyFactory.create(request.getPayload());
                return strategy.putMessage(request);
            } else {
                Audit.info("Receiver certificate not found, reverting to MSH.", markerFrom(message));
                return mshClient.sendEduMelding(request);
            }
        }
    }

    public boolean sendMessage(PutMessageRequestType request) {
        MDC.put(IntegrasjonspunktConfiguration.KEY_ORGANISATION_NUMBER, configuration.getOrganisationNumber());
        PutMessageRequestWrapper message = new PutMessageRequestWrapper(request);
        if (!message.hasSenderPartyNumber() && !configuration.hasOrganisationNumber()) {
            throw new MeldingsUtvekslingRuntimeException();
        }

        boolean result;
        if(hasAdresseregisterCertificate(message.getRecieverPartyNumber())) {
            Audit.info("Mottaker validert", markerFrom(message));
            PutMessageContext context = new PutMessageContext(eventLog, messageSender);
            PutMessageStrategyFactory putMessageStrategyFactory = PutMessageStrategyFactory.newInstance(context);

            PutMessageStrategy strategy = putMessageStrategyFactory.create(request.getPayload());
            PutMessageResponseType response = strategy.putMessage(request);
            result = validateResult(response);
        } else {
            Audit.info("Mottakers sertifikat mangler eller er ugyldig, prøver å sende melding via MSH", markerFrom(message));
            PutMessageResponseType response = mshClient.sendEduMelding(request);
            result = validateResult(response);
        }
        if(result) {
            Audit.info("Message successfully sent", markerFrom(message));
        } else {
            Audit.error("Message was not successfully sent", markerFrom(message));
        }
        return result;
    }

    public AdresseregisterVirksert getAdresseRegister() {
        return adresseregister;
    }

    public void setAdresseRegister(AdresseregisterVirksert adresseRegisterClient) {
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

    private static boolean validateResult(PutMessageResponseType response) {
        return "OK".equals(response.getResult().getType());
    }
}

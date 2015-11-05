package no.difi.meldingsutveksling.noarkexchange;


import com.thoughtworks.xstream.XStream;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.adresseregister.client.CertificateNotFoundException;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.Noekkelpar;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.ProcessState;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.putmessage.ErrorStatus;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.services.AdresseregisterService;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createErrorResponse;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createOkResponse;

@Component
public class MessageSender {

    @Autowired
    private EventLog eventLog;

    Logger log = Logger.getLogger(MessageSender.class.getName());

    @Autowired
    @Qualifier("multiTransport")
    private TransportFactory transportFactory;

    @Autowired
    private AdresseregisterService adresseregister;

    @Autowired
    private IntegrasjonspunktConfig configuration;

    @Autowired
    private IntegrasjonspunktNokkel keyInfo;

    @Autowired
    private StandardBusinessDocumentFactory standardBusinessDocumentFactory;

    boolean setSender(IntegrasjonspunktContext context, PutMessageRequestAdapter message) {

        if (!message.hasSenderPartyNumber() && !configuration.hasOrganisationNumber()) {
            throw new MeldingsUtvekslingRuntimeException();
        }

        if (!message.hasSenderPartyNumber()) {
            message.setSenderPartyNumber(configuration.getOrganisationNumber());
        }

        Certificate certificate = adresseregister.getCertificate(message.getSenderPartynumber());
        PrivateKey privatNoekkel = keyInfo.loadPrivateKey();
        Avsender avsender = Avsender.builder(new Organisasjonsnummer(message.getSenderPartynumber()), new Noekkelpar(privatNoekkel, certificate)).build();
        context.setAvsender(avsender);
        return true;
    }

    boolean setRecipient(IntegrasjonspunktContext context, String orgnr) {
        X509Certificate receiverCertificate;
        try {
            receiverCertificate = (X509Certificate) adresseregister.getCertificate(orgnr);

        } catch (CertificateNotFoundException e) {
            eventLog.log(new Event().setExceptionMessage(e.toString()));
            return false;
        }
        Mottaker mottaker = Mottaker.builder(new Organisasjonsnummer(orgnr), receiverCertificate).build();
        context.setMottaker(mottaker);
        return true;
    }

    public PutMessageResponseType sendMessage(PutMessageRequestType messageRequest) {
        PutMessageRequestAdapter message = new PutMessageRequestAdapter(messageRequest);
        if(!message.hasRecieverPartyNumber()) {
            log.severe(ErrorStatus.MISSING_RECIPIENT.toString());
            return createErrorResponse(ErrorStatus.MISSING_RECIPIENT);
        }


        JournalpostId p = JournalpostId.fromPutMessage(messageRequest);
        String journalPostId = p.value();

        IntegrasjonspunktContext context = new IntegrasjonspunktContext();
        context.setJpId(journalPostId);


        if (!setRecipient(context, message.getRecieverPartyNumber())) {
            log.info(ErrorStatus.CANNOT_RECIEVE + message.getRecieverPartyNumber());
            return createErrorResponse(ErrorStatus.CANNOT_RECIEVE);
        }

        if (!setSender(context, message)) {
            log.severe(ErrorStatus.MISSING_SENDER.toString());
            return createErrorResponse(ErrorStatus.MISSING_SENDER);
        }
        eventLog.log(new Event(ProcessState.SIGNATURE_VALIDATED));

        StandardBusinessDocument sbd;
        try {
            sbd = standardBusinessDocumentFactory.create(messageRequest, context.getAvsender(), context.getMottaker());

        } catch (IOException e) {
            eventLog.log(new Event().setJpId(journalPostId).setArkiveConversationId(message.getConversationId()).setProcessStates(ProcessState.MESSAGE_SEND_FAIL));
            log.severe("IO Error on Asic-e or sbd creation " + e.getMessage());
            return createErrorResponse(ErrorStatus.MISSING_SENDER);

        }
        Scope item = sbd.getStandardBusinessDocumentHeader().getBusinessScope().getScope().get(0);
        String hubCid = item.getInstanceIdentifier();
        eventLog.log(new Event().setJpId(journalPostId).setArkiveConversationId(message.getConversationId()).setHubConversationId(hubCid).setProcessStates(ProcessState.CONVERSATION_ID_LOGGED));

        Transport t = transportFactory.createTransport(sbd);
        t.send(configuration.getConfiguration(), sbd);

        eventLog.log(createOkStateEvent(messageRequest));

        return createOkResponse();
    }

    public void setAdresseregister(AdresseregisterService adresseregister) {
        this.adresseregister = adresseregister;
    }

    public void setEventLog(EventLog eventLog) {
        this.eventLog = eventLog;
    }

    public IntegrasjonspunktConfig getConfiguration() {
        return configuration;
    }

    public void setConfiguration(IntegrasjonspunktConfig configuration) {
        this.configuration = configuration;
    }

    public IntegrasjonspunktNokkel getKeyInfo() {
        return keyInfo;
    }

    public void setKeyInfo(IntegrasjonspunktNokkel keyInfo) {
        this.keyInfo = keyInfo;
    }

    public void setTransportFactory(TransportFactory transportFactory) {
        this.transportFactory = transportFactory;
    }

    private Event createOkStateEvent(PutMessageRequestType anyOject) {
        XStream xs = new XStream();
        Event event = new Event();
        event.setSender(anyOject.getEnvelope().getSender().getOrgnr());
        event.setReceiver(anyOject.getEnvelope().getReceiver().getOrgnr());
        event.setMessage(xs.toXML(anyOject));
        event.setProcessStates(ProcessState.SBD_SENT);
        return event;
    }

    public void setStandardBusinessDocumentFactory(StandardBusinessDocumentFactory standardBusinessDocumentFactory) {
        this.standardBusinessDocumentFactory = standardBusinessDocumentFactory;
    }

    public StandardBusinessDocumentFactory getStandardBusinessDocumentFactory() {
        return standardBusinessDocumentFactory;
    }

}


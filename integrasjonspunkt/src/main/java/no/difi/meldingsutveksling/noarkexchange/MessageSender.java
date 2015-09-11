package no.difi.meldingsutveksling.noarkexchange;


import com.thoughtworks.xstream.XStream;
import no.difi.asic.SignatureHelper;
import no.difi.meldingsutveksling.adresseregister.client.CertificateNotFoundException;
import no.difi.meldingsutveksling.domain.*;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.schema.*;
import no.difi.meldingsutveksling.oxalisexchange.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.services.AdresseregisterService;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

@Component
public class MessageSender {

    private Logger log = LoggerFactory.getLogger(MessageSender.class);

    @Autowired
    EventLog eventLog;

    @Autowired
    @Qualifier("multiTransport")
    TransportFactory transportFactory;

    @Autowired
    AdresseregisterService adresseregister;


    boolean setSender(IntegrasjonspunktContext context, AddressType sender) {
        if (sender == null) {
            return false;
        }
        Avsender avsender;
        Certificate sertifikat;
        try {
            sertifikat = adresseregister.getCertificate(sender.getOrgnr());
        } catch (CertificateNotFoundException e) {
            eventLog.log(new Event().setExceptionMessage(e.toString()));
            return false;
        }
        avsender = Avsender.builder(new Organisasjonsnummer(sender.getOrgnr()), new Noekkelpar(findPrivateKey(), sertifikat)).build();
        context.setAvsender(avsender);
        return true;
    }

    boolean setRecipient(IntegrasjonspunktContext context, AddressType receiver) {
        if (receiver == null) {
            return false;
        }
        X509Certificate receiverCertificate;
        try {
            receiverCertificate = (X509Certificate) adresseregister.getCertificate(receiver.getOrgnr());

        } catch (CertificateNotFoundException e) {
            eventLog.log(new Event().setExceptionMessage(e.toString()));
            return false;
        }
        Mottaker mottaker = Mottaker.builder(new Organisasjonsnummer(receiver.getOrgnr()), receiverCertificate).build();
        context.setMottaker(mottaker);
        return true;
    }

    public PutMessageResponseType sendMessage(PutMessageRequestType message) {

        String conversationId = message.getEnvelope().getConversationId();
        JournalpostId p = JournalpostId.fromPutMessage(message);
        String journalPostId = p.value();

        IntegrasjonspunktContext context = new IntegrasjonspunktContext();
        context.setJpId(journalPostId);

        EnvelopeType envelope = message.getEnvelope();
        if (envelope == null) {
            return createErrorResponse("Missing envelope");
        }

        AddressType receiver = message.getEnvelope().getReceiver();
        if (!setRecipient(context, receiver)) {
            return createErrorResponse("invalid recipient, no recipient or missing certificate for " + receiver.getOrgnr());
        }

        AddressType sender = message.getEnvelope().getSender();
        if (!setSender(context, sender)) {
            return createErrorResponse("invalid sender, no sender or missing certificate for " + receiver.getOrgnr());
        }
        eventLog.log(new Event(ProcessState.SIGNATURE_VALIDATED));

        SignatureHelper helper = new IntegrasjonspunktNokkel().getSignatureHelper();
        StandardBusinessDocument sbd;
        try {
            sbd = StandardBusinessDocumentFactory.create(message, helper, context.getAvsender(), context.getMottaker());

        } catch (IOException e) {
            eventLog.log(new Event().setJpId(journalPostId).setArkiveConversationId(conversationId).setProcessStates(ProcessState.MESSAGE_SEND_FAIL));
            return createErrorResponse("IO Error on Asic-e or sbd creation " + e.getMessage() + ", see log.");

        }
        Scope item = sbd.getStandardBusinessDocumentHeader().getBusinessScope().getScope().get(0);
        String hubCid = item.getInstanceIdentifier();
        eventLog.log(new Event().setJpId(journalPostId).setArkiveConversationId(conversationId).setHubConversationId(hubCid).setProcessStates(ProcessState.CONVERSATION_ID_LOGGED));

        Transport t = transportFactory.createTransport(sbd);
        t.send(sbd);

        eventLog.log(createOkStateEvent(message));

        return createOkResponse();
    }


    //TODO: Denne returnerer feil response, noe rart i generet kode
    //Type skal være ERROR, får ikke satt message i generert kode
    private PutMessageResponseType createErrorResponse(String message) {
        PutMessageResponseType response = new PutMessageResponseType();
        AppReceiptType receipt = new AppReceiptType();
        receipt.setType("ERROR");
        response.setResult(receipt);
        return response;
    }

    //TODO: Se på setting av melding, se i sammenheng med metoden over
    private PutMessageResponseType createOkResponse() {
        PutMessageResponseType response = new PutMessageResponseType();
        AppReceiptType receipt = new AppReceiptType();
        receipt.setType("OK");
        response.setResult(receipt);
        return response;
    }


    //todo refactor
    PrivateKey findPrivateKey() {
        IntegrasjonspunktNokkel nokkel = new IntegrasjonspunktNokkel();
        return nokkel.loadPrivateKey();
    }

    public void setAdresseregister(AdresseregisterService adresseregister) {
        this.adresseregister = adresseregister;
    }

    public void setEventLog(EventLog eventLog) {
        this.eventLog = eventLog;
    }

    private Event createErrorEvent(PutMessageRequestType anyOject, Exception e) {
        XStream xs = new XStream();
        Event event = new Event();
        event.setSender(anyOject.getEnvelope().getSender().getOrgnr());
        event.setReceiver(anyOject.getEnvelope().getReceiver().getOrgnr());
        event.setExceptionMessage(event.getMessage());
        event.setProcessStates(ProcessState.MESSAGE_SEND_FAIL);
        event.setMessage(xs.toXML(anyOject));
        return event;
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

}


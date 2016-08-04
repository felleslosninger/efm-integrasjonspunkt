package no.difi.meldingsutveksling.noarkexchange;


import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.Noekkelpar;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.difi.meldingsutveksling.services.Adresseregister;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createErrorResponse;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createOkResponse;

public class MessageSender {

    Logger log = LoggerFactory.getLogger(MessageSender.class);

    @Autowired
    private TransportFactory transportFactory;

    @Autowired
    private Adresseregister adresseregister;

    @Autowired
    private IntegrasjonspunktConfiguration configuration;

    @Autowired
    private Environment environment;

    @Autowired
    private IntegrasjonspunktNokkel keyInfo;

    @Autowired
    private StandardBusinessDocumentFactory standardBusinessDocumentFactory;

    public MessageSender() {
    }

    public MessageSender(TransportFactory transportFactory, Adresseregister adresseregister, IntegrasjonspunktConfiguration configuration, IntegrasjonspunktNokkel keyInfo, StandardBusinessDocumentFactory standardBusinessDocumentFactory) {
        this.transportFactory = transportFactory;
        this.adresseregister = adresseregister;
        this.configuration = configuration;
        this.keyInfo = keyInfo;
        this.standardBusinessDocumentFactory = standardBusinessDocumentFactory;
    }

    private Avsender createAvsender(PutMessageRequestWrapper message) throws MessageContextException {
        Certificate certificate;
        try {
            certificate = adresseregister.getCertificate(message.getSenderPartynumber());
        } catch (CertificateException e) {
            throw new MessageContextException(e, StatusMessage.MISSING_SENDER_CERTIFICATE);
        }
        PrivateKey privatNoekkel = keyInfo.loadPrivateKey();

        return Avsender.builder(new Organisasjonsnummer(message.getSenderPartynumber()), new Noekkelpar(privatNoekkel, certificate)).build();
    }

    private Mottaker createMottaker(String orgnr) throws MessageContextException {
        Certificate receiverCertificate;
        try {
            receiverCertificate = lookupCertificate(orgnr);
        } catch (CertificateException e) {
            throw new MessageContextException(e, StatusMessage.MISSING_RECIEVER_CERTIFICATE);
        }

        return Mottaker.builder(new Organisasjonsnummer(orgnr), receiverCertificate).build();
    }

    private Certificate lookupCertificate(String orgnr) throws CertificateException {
        Certificate certificate;
        certificate = adresseregister.getCertificate(orgnr);
        return certificate;
    }

    public PutMessageResponseType sendMessage(PutMessageRequestType messageRequest) {
        PutMessageRequestWrapper message = new PutMessageRequestWrapper(messageRequest);

        MessageContext messageContext;
        try {
            messageContext = createMessageContext(message);
            Audit.info("Required metadata validated", markerFrom(message));
        } catch (MessageContextException e) {
            log.error(markerFrom(message), e.getStatusMessage().getTechnicalMessage(), e);
            return createErrorResponse(e);
        }


        EduDocument edu;
        try {
            edu = standardBusinessDocumentFactory.create(messageRequest, messageContext.getConversationId(),  messageContext.getAvsender(), messageContext.getMottaker());
            Audit.info("EDUdocument created", markerFrom(message));
        } catch (MessageException e) {
            Audit.error("Failed to create EDUdocument", markerFrom(message));
            log.error(markerFrom(message), e.getStatusMessage().getTechnicalMessage(), e);
            return createErrorResponse(e);
        }

        Transport t = transportFactory.createTransport(edu);
        t.send(configuration.getConfiguration(), edu);

        Audit.info("Message sent", markerFrom(message));

        return createOkResponse();
    }

    /**
     * Creates MessageContext to contain data needed to send a message such as
     * sender/recipient party numbers and certificates
     *
     * The context also contains error statuses if the message request has validation errors.
     *
     * @param message that contains sender, receiver and journalpost id
     * @return MessageContext containing data about the shipment
     */
    protected MessageContext createMessageContext(PutMessageRequestWrapper message) throws MessageContextException {
        MessageContext context = new MessageContext();

        if(!message.hasRecieverPartyNumber()) {
            throw new MessageContextException(StatusMessage.MISSING_RECIEVER_ORGANIZATION_NUMBER);
        }
        Avsender avsender;
        final Mottaker mottaker;
        avsender = createAvsender(message);
        mottaker = createMottaker(message.getRecieverPartyNumber());

        PutMessageRequestWrapper.MessageType type = message.getMessageType();

        JournalpostId id;
        try {
            id = JournalpostId.fromPutMessage(message);
        } catch (PayloadException e) {
            Audit.error("Unknown payload string", markerFrom(message));
            log.error(markerFrom(message), e.getMessage(), e);
            throw new IllegalArgumentException(e.getMessage());
        }
        context.setJpId(id.value());

        String converationId = message.getConversationId();

        context.setMottaker(mottaker);
        context.setAvsender(avsender);
        context.setConversationId(converationId);
        return context;
    }

    public void setAdresseregister(Adresseregister adresseregister) {
        this.adresseregister = adresseregister;
    }

    public IntegrasjonspunktConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(IntegrasjonspunktConfiguration configuration) {
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

    public void setStandardBusinessDocumentFactory(StandardBusinessDocumentFactory standardBusinessDocumentFactory) {
        this.standardBusinessDocumentFactory = standardBusinessDocumentFactory;
    }

    public StandardBusinessDocumentFactory getStandardBusinessDocumentFactory() {
        return standardBusinessDocumentFactory;
    }

    public Environment getEnvironment() {
        return environment;
    }
}


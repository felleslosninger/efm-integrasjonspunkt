package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.Noekkelpar;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.services.Adresseregister;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.core.EDUCoreMarker.markerFrom;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createErrorResponse;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createOkResponse;

public class MessageSender implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(MessageSender.class);

    private TransportFactory transportFactory;

    private Adresseregister adresseregister;

    private IntegrasjonspunktProperties properties;

    private ApplicationContext context;

    private IntegrasjonspunktNokkel keyInfo;

    private StandardBusinessDocumentFactory standardBusinessDocumentFactory;

    private ServiceRegistryLookup serviceRegistryLookup;

    public MessageSender() {
    }

    public MessageSender(TransportFactory transportFactory, Adresseregister adresseregister,
                         IntegrasjonspunktProperties properties, IntegrasjonspunktNokkel keyInfo,
                         StandardBusinessDocumentFactory standardBusinessDocumentFactory, ServiceRegistryLookup serviceRegistryLookup) {
        this.transportFactory = transportFactory;
        this.adresseregister = adresseregister;
        this.properties = properties;
        this.keyInfo = keyInfo;
        this.standardBusinessDocumentFactory = standardBusinessDocumentFactory;
        this.serviceRegistryLookup = serviceRegistryLookup;
    }

    private Avsender createAvsender(String identifier) throws MessageContextException {
        ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecord(identifier);
        Certificate certificate;
        try {
            certificate = adresseregister.getCertificate(serviceRecord);
        } catch (CertificateException e) {
            throw new MessageContextException(e, StatusMessage.MISSING_SENDER_CERTIFICATE);
        }
        PrivateKey privatNoekkel = keyInfo.loadPrivateKey();

        return Avsender.builder(new Organisasjonsnummer(identifier), new Noekkelpar(privatNoekkel, certificate)).build();
    }

    private Mottaker createMottaker(String identifier) throws MessageContextException {
        Certificate receiverCertificate;
        try {
            receiverCertificate = lookupCertificate(identifier);
        } catch (CertificateException e) {
            throw new MessageContextException(e, StatusMessage.MISSING_RECIEVER_CERTIFICATE);
        }

        return Mottaker.builder(new Organisasjonsnummer(identifier), receiverCertificate).build();
    }

    private Certificate lookupCertificate(String orgnr) throws CertificateException {
        ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecord(orgnr);
        Certificate certificate;
        certificate = adresseregister.getCertificate(serviceRecord);
        return certificate;
    }

    public PutMessageResponseType sendMessage(EDUCore message) {
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
            edu = standardBusinessDocumentFactory.create(message, messageContext.getConversationId(), messageContext.getAvsender(), messageContext.getMottaker());
            Audit.info("EDUdocument created", markerFrom(message));
        } catch (MessageException e) {
            Audit.error("Failed to create EDUdocument", markerFrom(message), e);
            log.error(markerFrom(message), e.getStatusMessage().getTechnicalMessage(), e);
            return createErrorResponse(e);
        }

        Transport t = transportFactory.createTransport(edu);
        t.send(context, edu);

        Audit.info("Message sent", markerFrom(message));

        return createOkResponse();
    }

    public void sendMessage(ConversationResource conversation) throws MessageContextException {
        MessageContext messageContext = createMessageContext(conversation);

        EduDocument edu;
        try {
            edu = standardBusinessDocumentFactory.create(conversation, messageContext);
            log.info("EduMessage created from ConversationResource");
        } catch (MessageException e) {
            log.error("Failed creating EduMessage from ConversationResource", e);
            return;
        }

        Transport t = transportFactory.createTransport(edu);
        t.send(context, edu);

        log.info("ConversationResource sent");
    }

    public MessageContext createMessageContext(ConversationResource conversation) throws MessageContextException {
        if (isNullOrEmpty(conversation.getReceiverId())) {
            throw new MessageContextException(StatusMessage.MISSING_RECIEVER_ORGANIZATION_NUMBER);
        }

        MessageContext context = new MessageContext();
        context.setAvsender(createAvsender(conversation.getSenderId()));
        context.setMottaker(createMottaker(conversation.getReceiverId()));
        context.setJpId("");
        context.setConversationId(conversation.getConversationId());

        return context;
    }

    /**
     * Creates MessageContext to contain data needed to send a message such as sender/recipient party numbers and certificates
     *
     * The context also contains error statuses if the message request has validation errors.
     *
     * @param message that contains sender, receiver and journalpost id
     * @return MessageContext containing data about the shipment
     */
    protected MessageContext createMessageContext(EDUCore message) throws MessageContextException {
        if (isNullOrEmpty(message.getReceiver().getIdentifier())) {
            throw new MessageContextException(StatusMessage.MISSING_RECIEVER_ORGANIZATION_NUMBER);
        }

        MessageContext messageContext = new MessageContext();

        Avsender avsender;
        final Mottaker mottaker;
        avsender = createAvsender(message.getSender().getIdentifier());
        mottaker = createMottaker(message.getReceiver().getIdentifier());

        if (message.getMessageType() == EDUCore.MessageType.EDU) {
            messageContext.setJpId(message.getPayloadAsMeldingType().getJournpost().getJpId());
        } else {
            messageContext.setJpId("");
        }

        String converationId = message.getId();

        messageContext.setMottaker(mottaker);
        messageContext.setAvsender(avsender);
        messageContext.setConversationId(converationId);
        return messageContext;
    }

    public void setAdresseregister(Adresseregister adresseregister) {
        this.adresseregister = adresseregister;
    }

    public IntegrasjonspunktProperties getProperties() {
        return properties;
    }

    public void setProperties(IntegrasjonspunktProperties properties) {
        this.properties = properties;
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

    public ServiceRegistryLookup getServiceRegistryLookup() {
        return serviceRegistryLookup;
    }

    public void setServiceRegistryLookup(ServiceRegistryLookup serviceRegistryLookup) {
        this.serviceRegistryLookup = serviceRegistryLookup;
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        this.context = ac;
    }

}

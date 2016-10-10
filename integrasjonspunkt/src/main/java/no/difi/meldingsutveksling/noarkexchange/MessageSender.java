package no.difi.meldingsutveksling.noarkexchange;

import com.google.common.base.Strings;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import static no.difi.meldingsutveksling.core.EDUCoreMarker.markerFrom;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.Noekkelpar;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.logging.Audit;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createErrorResponse;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createOkResponse;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.services.Adresseregister;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class MessageSender {

    private static final Logger log = LoggerFactory.getLogger(MessageSender.class);

    private TransportFactory transportFactory;

    private Adresseregister adresseregister;

    private IntegrasjonspunktProperties properties;

    private ApplicationContext context;

    private IntegrasjonspunktNokkel keyInfo;

    private StandardBusinessDocumentFactory standardBusinessDocumentFactory;

    public MessageSender() {
    }

    public MessageSender(TransportFactory transportFactory, Adresseregister adresseregister, IntegrasjonspunktProperties properties, IntegrasjonspunktNokkel keyInfo, StandardBusinessDocumentFactory standardBusinessDocumentFactory) {
        this.transportFactory = transportFactory;
        this.adresseregister = adresseregister;
        this.properties = properties;
        this.keyInfo = keyInfo;
        this.standardBusinessDocumentFactory = standardBusinessDocumentFactory;
    }

    private Avsender createAvsender(EDUCore message) throws MessageContextException {
        Certificate certificate;
        try {
            certificate = adresseregister.getCertificate(message.getSender().getOrgNr());
        } catch (CertificateException e) {
            throw new MessageContextException(e, StatusMessage.MISSING_SENDER_CERTIFICATE);
        }
        PrivateKey privatNoekkel = keyInfo.loadPrivateKey();

        return Avsender.builder(new Organisasjonsnummer(message.getSender().getOrgNr()), new Noekkelpar(privatNoekkel, certificate)).build();
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
            Audit.error("Failed to create EDUdocument", markerFrom(message));
            log.error(markerFrom(message), e.getStatusMessage().getTechnicalMessage(), e);
            return createErrorResponse(e);
        }

        Transport t = transportFactory.createTransport(edu);
        t.send(context, edu);

        Audit.info("Message sent", markerFrom(message));

        return createOkResponse();
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
        if (Strings.isNullOrEmpty(message.getReceiver().getOrgNr())) {
            throw new MessageContextException(StatusMessage.MISSING_RECIEVER_ORGANIZATION_NUMBER);
        }

        MessageContext context = new MessageContext();

        Avsender avsender;
        final Mottaker mottaker;
        avsender = createAvsender(message);
        mottaker = createMottaker(message.getReceiver().getOrgNr());

        if (message.getMessageType() == EDUCore.MessageType.EDU) {
            context.setJpId(message.getPayloadAsMeldingType().getJournpost().getJpId());
        } else {
            context.setJpId("");
        }

        String converationId = message.getId();

        context.setMottaker(mottaker);
        context.setAvsender(avsender);
        context.setConversationId(converationId);
        return context;
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
}

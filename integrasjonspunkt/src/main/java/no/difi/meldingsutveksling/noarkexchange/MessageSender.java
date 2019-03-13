package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.AsicHandler;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.io.InputStream;

import static no.difi.meldingsutveksling.core.EDUCoreMarker.markerFrom;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createErrorResponse;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createOkResponse;

@Component
public class MessageSender implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(MessageSender.class);

    private TransportFactory transportFactory;
    private ApplicationContext context;
    private StandardBusinessDocumentFactory standardBusinessDocumentFactory;
    private AsicHandler asicHandler;
    private MessageContextFactory messageContextFactory;

    public MessageSender(TransportFactory transportFactory,
                         StandardBusinessDocumentFactory standardBusinessDocumentFactory,
                         AsicHandler asicHandler,
                         MessageContextFactory messageContextFactory) {
        this.transportFactory = transportFactory;
        this.standardBusinessDocumentFactory = standardBusinessDocumentFactory;
        this.asicHandler = asicHandler;
        this.messageContextFactory = messageContextFactory;
    }

    public PutMessageResponseType sendMessage(EDUCore message) {
        MessageContext messageContext;
        try {
            messageContext = messageContextFactory.from(message);
            Audit.info("Required metadata validated", markerFrom(message));
        } catch (MessageContextException e) {
            log.error(markerFrom(message), e.getStatusMessage().getTechnicalMessage(), e);
            return createErrorResponse(e);
        }

        StandardBusinessDocument edu;
        try {
            edu = standardBusinessDocumentFactory.create(message, messageContext.getConversationId(), messageContext.getAvsender(), messageContext.getMottaker());
            Audit.info("StandardBusinessDocument created", markerFrom(message));
        } catch (MessageException e) {
            Audit.error("Failed to create StandardBusinessDocument", markerFrom(message), e);
            log.error(markerFrom(message), e.getStatusMessage().getTechnicalMessage(), e);
            return createErrorResponse(e);
        }

        Transport t = transportFactory.createTransport(edu);
        t.send(context, edu);

        return createOkResponse();
    }


    public void sendMessage(NextMoveMessage message) throws MessageContextException, NextMoveException {
        MessageContext messageContext = messageContextFactory.from(message);
        InputStream is = asicHandler.createEncryptedAsic(message, messageContext);
        Transport transport = transportFactory.createTransport(message.getSbd());
        transport.send(this.context, message.getSbd(), is);
    }


    public void sendMessage(ConversationResource conversation) throws MessageContextException {
        MessageContext messageContext = messageContextFactory.from(conversation);

        StandardBusinessDocument edu;
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

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        this.context = ac;
    }

}

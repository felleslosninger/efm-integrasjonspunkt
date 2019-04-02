package no.difi.meldingsutveksling.noarkexchange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ApplicationContextHolder;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.AsicHandler;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;

import static no.difi.meldingsutveksling.core.EDUCoreMarker.markerFrom;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createErrorResponse;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createOkResponse;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageSender {

    private final TransportFactory transportFactory;
    private final StandardBusinessDocumentFactory standardBusinessDocumentFactory;
    private final AsicHandler asicHandler;
    private final MessageContextFactory messageContextFactory;
    private final ApplicationContextHolder applicationContextHolder;

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
        t.send(applicationContextHolder.getApplicationContext(), edu);

        return createOkResponse();
    }


    public void sendMessage(NextMoveMessage message) throws MessageContextException {
        MessageContext messageContext = messageContextFactory.from(message);
        InputStream is = asicHandler.createEncryptedAsic(message, messageContext);
        Transport transport = transportFactory.createTransport(message.getSbd());
        transport.send(applicationContextHolder.getApplicationContext(), message.getSbd(), is);
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
        t.send(applicationContextHolder.getApplicationContext(), edu);

        log.info("ConversationResource sent");
    }
}

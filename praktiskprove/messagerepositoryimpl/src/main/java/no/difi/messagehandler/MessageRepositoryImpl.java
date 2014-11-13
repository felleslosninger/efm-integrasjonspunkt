package no.difi.messagehandler;

import eu.peppol.PeppolMessageMetaData;
import eu.peppol.persistence.MessageRepository;
import eu.peppol.persistence.OxalisMessagePersistenceException;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import org.w3c.dom.Document;


import java.io.InputStream;
import java.security.GeneralSecurityException;

/**
 * @author Kubilay Karayilan
 *         Kubilay.Karayilan@inmeta.no
 *         created on 11.11.2014.
 */
public class MessageRepositoryImpl implements MessageRepository {
    private EventLog eventLog = EventLog.create();
    @Override
    public void saveInboundMessage(PeppolMessageMetaData peppolMessageMetaData, Document document) throws OxalisMessagePersistenceException {
        MessageReceieverTemplate template = new OxalisMessageReceiverTemplate();
        try {
            template.receive(peppolMessageMetaData,document);
        } catch (GeneralSecurityException e) {
            eventLog.log(new Event().setException(e));
        }
    }

    @Override
    public void saveInboundMessage(PeppolMessageMetaData peppolMessageMetaData, InputStream inputStream) throws OxalisMessagePersistenceException {
        throw new UnsupportedOperationException();
    }
}

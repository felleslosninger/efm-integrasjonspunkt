package no.difi.messagehandler;

import eu.peppol.PeppolMessageMetaData;
import eu.peppol.persistence.MessageRepository;
import eu.peppol.persistence.OxalisMessagePersistenceException;
import org.w3c.dom.Document;


import java.io.InputStream;

/**
 * @author Kubilay Karayilan
 *         Kubilay.Karayilan@inmeta.no
 *         created on 11.11.2014.
 */
public class MessageRepositoryImpl implements MessageRepository {
    @Override
    public void saveInboundMessage(PeppolMessageMetaData peppolMessageMetaData, Document document) throws OxalisMessagePersistenceException {

    }

    @Override
    public void saveInboundMessage(PeppolMessageMetaData peppolMessageMetaData, InputStream inputStream) throws OxalisMessagePersistenceException {
        throw new UnsupportedOperationException();
    }
}

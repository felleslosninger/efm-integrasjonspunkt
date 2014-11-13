package no.difi.messagehandler;

import eu.peppol.PeppolMessageMetaData;
import eu.peppol.persistence.MessageRepository;
import eu.peppol.persistence.OxalisMessagePersistenceException;
import org.w3c.dom.Document;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

/**
 * @author Kubilay Karayilan
 *         Kubilay.Karayilan@inmeta.no
 *         created on 11.11.2014.
 */
public class MessageRepositoryImpl implements MessageRepository {
    @Override
    public void saveInboundMessage(PeppolMessageMetaData peppolMessageMetaData, Document document) throws OxalisMessagePersistenceException {
        MessageReceieverTemplate template = new OxalisMessageReceiverTemplate();
        try {
            template.receive(peppolMessageMetaData,document);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveInboundMessage(PeppolMessageMetaData peppolMessageMetaData, InputStream inputStream) throws OxalisMessagePersistenceException {
        throw new NotImplementedException();
    }
}

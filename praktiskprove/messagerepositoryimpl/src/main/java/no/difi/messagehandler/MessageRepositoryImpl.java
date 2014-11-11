package no.difi.messagehandler;

import eu.peppol.PeppolMessageMetaData;
import eu.peppol.persistence.MessageRepository;
import eu.peppol.persistence.OxalisMessagePersistenceException;
import no.difi.messagehandler.MessageReceieverTemplate;
import no.difi.messagehandler.OxalisMessageReceiverTemplate;
import org.w3c.dom.Document;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.InputStream;

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
            try {
                template.receive(peppolMessageMetaData, document);
            } catch (JAXBException e) {
                e.printStackTrace();
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveInboundMessage(PeppolMessageMetaData peppolMessageMetaData, InputStream inputStream) throws OxalisMessagePersistenceException {
        throw new NotImplementedException();
    }
}

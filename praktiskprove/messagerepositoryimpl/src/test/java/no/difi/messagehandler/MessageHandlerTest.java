package no.difi.messagehandler;

import eu.peppol.PeppolMessageMetaData;
import eu.peppol.persistence.MessageRepository;
import eu.peppol.persistence.OxalisMessagePersistenceException;
import org.bouncycastle.cms.CMSException;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.junit.Assert.assertEquals;

/**
 * Created by kons-kka on 31.10.2014.
 */
public class MessageHandlerTest {
    @Test
    public void handlerTest() throws JAXBException, GeneralSecurityException, IOException, CMSException, OxalisMessagePersistenceException, ParserConfigurationException, SAXException {
        String textToEncrypt = "Java rules";
        ClassLoader classLoader = getClass().getClassLoader();
         MessageHandler messageHandler = new MessageHandler();

         //File file = new File(classLoader.getResource("sbdUt.xml").getFile());
         File file1= new File(
                 System.getProperty("user.home")+File.separator + "Dropbox" + File.separator +"DifiCmnDocs" +
                    File.separator + "demo"+ File.separator+ "sbdUt.xml");
          messageHandler.unmarshall(file1);
     /*  // assertEquals(textToEncrypt, messageHandler.cryptAtext(textToEncrypt));
         DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        MessageRepositoryImpl messageRepository = new MessageRepositoryImpl();
        messageRepository.saveInboundMessage(new PeppolMessageMetaData(), doc);*/
    }
}

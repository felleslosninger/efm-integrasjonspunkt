package no.difi.messagehandler;

import org.bouncycastle.cms.CMSException;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Created by kons-kka on 31.10.2014.
 */
public class MessageHandlerTest {
    @Test
    public void handlerTest() throws JAXBException, GeneralSecurityException, IOException, CMSException {
        MessageHandler messageHandler = new MessageHandler();
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("sbd.xml").getFile());
        //messageHandler.unmarshall(file);
        messageHandler.codeDecode();

    }
}

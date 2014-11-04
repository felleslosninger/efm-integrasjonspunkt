package no.difi.messagehandler;

import org.bouncycastle.cms.CMSException;
import org.junit.Test;


import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * Created by kons-kka on 31.10.2014.
 */
public class MessageHandlerTest {

    private void handlerTest() throws JAXBException, NoSuchAlgorithmException, NoSuchPaddingException, CertificateException, InvalidKeyException, KeyStoreException, BadPaddingException, IllegalBlockSizeException, UnrecoverableEntryException, IOException, NoSuchProviderException, CMSException {
        MessageHandler messageHandler = new MessageHandler();
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("sbd.xml").getFile());
        messageHandler.unmarshall(file);

    }
}

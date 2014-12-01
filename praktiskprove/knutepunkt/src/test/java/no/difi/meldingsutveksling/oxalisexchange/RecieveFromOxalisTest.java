package no.difi.meldingsutveksling.oxalisexchange;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Created by kubkaray on 20.11.2014.
 */
public class RecieveFromOxalisTest {
    @Ignore
    @Test
    public void recieve( ) {
        OxalisMessageReceiverTemplate oxalisMessageReceiverTemplate = new OxalisMessageReceiverTemplate();
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("sbdUt.xml").getFile());
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            oxalisMessageReceiverTemplate.receive(doc);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

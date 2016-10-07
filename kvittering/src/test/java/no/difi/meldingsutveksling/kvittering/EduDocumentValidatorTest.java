package no.difi.meldingsutveksling.kvittering;

import org.apache.xml.security.exceptions.XMLSecurityException;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

import static org.junit.Assert.assertTrue;


/**
 * @author Glenn Bech
 */
public class EduDocumentValidatorTest {

    @Test
    public void should_verify_external_document_that_is_valid() throws XMLSignatureException, ParserConfigurationException, SAXException, IOException, XMLSecurityException, XPathExpressionException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder builder;
        builder = dbf.newDocumentBuilder();
        Document doc = builder.parse(EduDocumentValidatorTest.class.getClassLoader().getResourceAsStream("signed_sbd_kvittering.xml"));
        assertTrue(DocumentValidator.validate(doc));
    }
}
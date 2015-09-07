package no.difi.meldingsutveksling.noarkexchange.schema;

import com.sun.org.apache.xerces.internal.dom.ElementNSImpl;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Wrapper for a NOARK JournalpostID
 *
 * @author Glenn Bech
 */
public class JournalpostId {

    private static final String JP_ID = "jpId";
    private static final String DATA = "data";

    private String jpId;

    private JournalpostId(String jpId) {
        this.jpId = jpId;
    }

    public static JournalpostId fromPutMessage(PutMessageRequestType message) {
        JournalpostId result;
        Object payload = message.getPayload();
        if (payload instanceof String) {
            String jp = extractJpId((String) payload);
            result = new JournalpostId(jp);
        } else if (payload instanceof ElementNSImpl) {
            return new JournalpostId(getJpIdFromParentNode(message));
        } else {
            throw new MeldingsUtvekslingRuntimeException("The payload XML element can neither be parsed as a String nor an  XML element. ");
        }
        return result;
    }

    public String value() {
        return jpId;
    }

    private static String getJpIdFromParentNode(PutMessageRequestType message) {
        Document document = getDocument(message);
        NodeList messageElement = document.getElementsByTagName(JP_ID);
        if (messageElement.getLength() == 0) {
            throw new MeldingsUtvekslingRuntimeException("no " + JP_ID + " element in document ");
        }
        return messageElement.item(0).getTextContent();
    }

    private static Document getDocument(PutMessageRequestType message) throws MeldingsUtvekslingRuntimeException {
        DocumentBuilder documentBuilder = getDocumentBuilder();
        Element element = (Element) message.getPayload();
        NodeList nodeList = element.getElementsByTagName(DATA);
        if (nodeList.getLength() == 0) {
            throw new MeldingsUtvekslingRuntimeException("no " + DATA + " element in payload");
        }
        Node payloadData = nodeList.item(0);
        String payloadDataTextContent = payloadData.getTextContent();
        Document document;

        try {
            document = documentBuilder.parse(new InputSource(new ByteArrayInputStream(payloadDataTextContent.getBytes("utf-8"))));
        } catch (SAXException | IOException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        return document;
    }


    private static DocumentBuilder getDocumentBuilder() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        return builder;
    }

    private static String extractJpId(String from) {
        String result = from.substring(from.indexOf("<jpId>") + "<jpId>".length(), from.indexOf("</jpId>"));
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JournalpostId that = (JournalpostId) o;

        if (jpId != null ? !jpId.equals(that.jpId) : that.jpId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return jpId != null ? jpId.hashCode() : 0;
    }


}

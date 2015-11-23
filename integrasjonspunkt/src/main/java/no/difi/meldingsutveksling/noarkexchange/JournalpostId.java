package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.apache.commons.lang.StringEscapeUtils.unescapeHtml;

/**
 * Wrapper for a NOARK JournalpostID. The class will extract The NOARK jpID field for different
 * payload styles
 *
 * @author Glenn Bech
 */
public class JournalpostId {

    private String jpId;

    private JournalpostId(String jpId) {
        this.jpId = jpId;
    }

    public static JournalpostId fromPutMessage(PutMessageRequestAdapter message) {
        JournalpostId result;
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        try {
            XPathExpression expression = xPath.compile("/Melding/journpost/jpId");
            String doc;
            if(message.getPayload() instanceof String){
                doc = (String) message.getPayload();
                doc = unescapeHtml(doc);
            } else {
                doc = ((Node) message.getPayload()).getFirstChild().getTextContent().trim();
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(doc.getBytes(java.nio.charset.Charset.forName("utf-8"))));
            result = new JournalpostId(expression.evaluate(document));
        } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
            throw new MeldingsUtvekslingRuntimeException("Could not extract jpId from the payload", e);
        }
        return result;
    }

    public String value() {
        return jpId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JournalpostId that = (JournalpostId) o;

        return !(jpId != null ? !jpId.equals(that.jpId) : that.jpId != null);

    }

    @Override
    public int hashCode() {
        return jpId != null ? jpId.hashCode() : 0;
    }


}

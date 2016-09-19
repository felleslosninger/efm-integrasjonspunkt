package no.difi.meldingsutveksling.noarkexchange;

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

import static no.difi.meldingsutveksling.noarkexchange.PayloadUtil.queryPayload;
import static org.apache.commons.lang.StringEscapeUtils.unescapeHtml;

/**
 * Wrapper for a NOARK JournalpostID. The class will extract The NOARK jpID field for different
 * payload styles
 *
 * @author Glenn Bech
 */
class JournalpostId {

    private String jpId;
    private static String jpIdXpath= "/Melding/journpost/jpId";

    public JournalpostId(String jpId) {
        this.jpId = jpId;
    }

    public static JournalpostId fromPutMessage(PutMessageRequestWrapper message) throws PayloadException {
        return new JournalpostId(queryPayload(message, jpIdXpath));
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

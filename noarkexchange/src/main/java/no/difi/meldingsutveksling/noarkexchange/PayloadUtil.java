package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.util.StringUtils;
import org.springframework.xml.transform.StringSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
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

public class PayloadUtil {
    public static final String APP_RECEIPT_INDICATOR = "AppReceipt";
    public static final String PAYLOAD_UNKNOWN_TYPE = "Payload is of unknown type cannot determine what type of message it is";

    public static boolean isAppReceipt(Object payload) {
        if (payload instanceof AppReceiptType) {
            return true;
        }
        if(payload instanceof String) {
            return ((String) payload).contains(APP_RECEIPT_INDICATOR);
        }
        if(payload instanceof Node) {
            Node firstChild = ((Node) payload).getFirstChild();
            if (firstChild != null) {
                final String nodeName = firstChild.getTextContent();
                return nodeName.contains(APP_RECEIPT_INDICATOR);
            }
        }
        return false;
    }

    public static String payloadAsString(Object payload) {
        if(payload instanceof String) {
            return ((String) payload);
        } else if (payload instanceof Node) {
            return ((Node) payload).getFirstChild().getTextContent();
        } else {
            throw new RuntimeException("Could not get payload as String");
        }
    }

    public static boolean isEmpty(Object payload) {
        if (payload instanceof String) {
            return StringUtils.isEmpty(payload);
        } else if (payload instanceof Node) {
           return  !((Node) payload).hasChildNodes();
        } else {
            throw new RuntimeException(PAYLOAD_UNKNOWN_TYPE);
        }
    }

    public static AppReceiptType getAppReceiptType(Object payload) throws JAXBException {
        final String payloadAsString = payloadAsString(payload);

        StringSource source = new StringSource(payloadAsString);
        JAXBContext jaxbContext = JAXBContext.newInstance("no.difi.meldingsutveksling.noarkexchange.schema");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        JAXBElement<AppReceiptType> r = unmarshaller.unmarshal(source, AppReceiptType.class);
        return r.getValue();
    }

    public static String queryPayload(PutMessageRequestWrapper message, String xpath) throws PayloadException {
        String result;

        if(message.getMessageType() != PutMessageRequestWrapper.MessageType.EDUMESSAGE){
            return "";
        }

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        try {
            XPathExpression expression = xPath.compile(xpath);
            String doc;
            if (message.getPayload() instanceof String) {
                doc = (String) message.getPayload();
                doc = unescapeHtml(doc);
            } else {
                doc = ((Node) message.getPayload()).getFirstChild().getTextContent().trim();
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(doc.getBytes(java.nio.charset.Charset.forName("utf-8"))));
            result = expression.evaluate(document);
        } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
            throw new PayloadException("Could not execute query \'"+xpath+"\'  on the payload");
        }
        return result;
    }

    public static Object unmarshallPayload(Object payload) throws JAXBException {
        String p;
        Object msg;

        if (payload instanceof String) {
            p = (String) payload;
            p = StringEscapeUtils.unescapeHtml(p);
        } else {
            p = ((Node) payload).getFirstChild().getTextContent().trim();
        }

        if (PayloadUtil.isAppReceipt(payload)) {
            JAXBContext jaxbContext = JAXBContext.newInstance("no.difi.meldingsutveksling.noarkexchange.schema");
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            msg = unmarshaller.unmarshal(new StringSource(p), AppReceiptType.class).getValue();
        } else {
            JAXBContext jaxbContext = JAXBContext.newInstance(MeldingType.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            msg = unmarshaller.unmarshal(new StringSource(p), MeldingType.class).getValue();
        }
        return msg;
    }
}

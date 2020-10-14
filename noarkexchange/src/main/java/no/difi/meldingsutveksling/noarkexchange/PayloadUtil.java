package no.difi.meldingsutveksling.noarkexchange;

import com.google.common.base.Strings;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@UtilityClass
public class PayloadUtil {

    private static final String APP_RECEIPT_INDICATOR = "AppReceipt";
    private static final String PAYLOAD_UNKNOWN_TYPE = "Payload is of unknown type cannot determine what type of message it is";

    JAXBContext jaxbContext;

    static {
        try {
            jaxbContext = JAXBContext.newInstance("no.difi.meldingsutveksling.noarkexchange.schema");
        } catch (JAXBException e) {
            log.error("Could not create JAXBContext", e);
        }
    }

    public static boolean isAppReceipt(Object payload) {
        if (payload instanceof AppReceiptType) {
            return true;
        }
        if (payload instanceof String) {
            return ((String) payload).contains(APP_RECEIPT_INDICATOR);
        }
        if (payload instanceof Node) {
            Node firstChild = ((Node) payload).getFirstChild();
            if (firstChild != null) {
                final String nodeName = firstChild.getTextContent();
                return nodeName.contains(APP_RECEIPT_INDICATOR);
            }
        }
        return false;
    }

    public static String payloadAsString(Object payload) {
        if (payload instanceof String) {
            return ((String) payload);
        } else if (payload instanceof Node) {
            return ((Node) payload).getFirstChild().getTextContent();
        } else {
            throw new NoarkPayloadRuntimeException("Could not get payload as String");
        }
    }

    public static boolean isEmpty(Object payload) {
        if (payload instanceof String) {
            return Strings.isNullOrEmpty((String)payload);
        } else if (payload instanceof Node) {
            return !((Node) payload).hasChildNodes();
        } else {
            throw new NoarkPayloadRuntimeException(PAYLOAD_UNKNOWN_TYPE);
        }
    }

    static AppReceiptType getAppReceiptType(Object payload) throws JAXBException {
        final String payloadAsString = payloadAsString(payload);
        ByteArrayInputStream bis = new ByteArrayInputStream(payloadAsString.getBytes(StandardCharsets.UTF_8));
        JAXBContext jaxbContext = JAXBContext.newInstance("no.difi.meldingsutveksling.noarkexchange.schema");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        JAXBElement<AppReceiptType> r = unmarshaller.unmarshal(new StreamSource(bis), AppReceiptType.class);
        return r.getValue();
    }

    public static String queryPayload(Object payload, String xpath) throws PayloadException {
        String result;

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        try {
            XPathExpression expression = xPath.compile(xpath);
            String doc = getDoc(payload);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8)));
            result = expression.evaluate(document);
        } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
            throw new PayloadException("Could not execute query \'" + xpath + "\'  on the payload");
        }
        return result;
    }

    public static List<NoarkDocument> parsePayloadForDocuments(Object payload) throws PayloadException {
        List<NoarkDocument> docs = new ArrayList<>();

        try {
            XMLEventReader eventReader = getEventReader(payload);
            NoarkDocument noarkDocument = null;
            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    StartElement start = event.asStartElement();
                    switch (start.getName().getLocalPart()) {
                        case "dokument":
                            noarkDocument = new NoarkDocument();
                            break;
                        case "veFilnavn":
                            event = setFilename(eventReader, noarkDocument);
                            break;
                        case "veMimeType":
                            event = setContentType(eventReader, noarkDocument);
                            break;
                        case "dbTittel":
                            event = setTitle(eventReader, noarkDocument);
                            break;
                        case "base64":
                            event = setContent(eventReader, noarkDocument);
                            break;
                        default:
                            break;
                    }
                }
                if (event.isEndElement()) {
                    EndElement end = event.asEndElement();
                    if ("dokument".equals(end.getName().getLocalPart()) && noarkDocument != null) {
                        docs.add(noarkDocument);
                    }
                }
            }
            return docs;
        } catch (XMLStreamException e) {
            throw new PayloadException("Error parsing payload", e);
        }
    }

    private static XMLEvent setContent(XMLEventReader eventReader, NoarkDocument noarkDocument) throws XMLStreamException {
        XMLEvent event;
        event = eventReader.nextEvent();
        if (noarkDocument != null) {
            noarkDocument.setContent(event.asCharacters().getData().getBytes(StandardCharsets.UTF_8));
        }
        return event;
    }

    private static XMLEvent setTitle(XMLEventReader eventReader, NoarkDocument noarkDocument) throws XMLStreamException {
        XMLEvent event;
        event = eventReader.nextEvent();
        if (noarkDocument != null) {
            noarkDocument.setTitle(event.asCharacters().getData());
        }
        return event;
    }

    private static XMLEvent setContentType(XMLEventReader eventReader, NoarkDocument noarkDocument) throws XMLStreamException {
        XMLEvent event;
        event = eventReader.nextEvent();
        if (noarkDocument != null) {
            noarkDocument.setContentType(event.asCharacters().getData());
        }
        return event;
    }

    private static XMLEvent setFilename(XMLEventReader eventReader, NoarkDocument noarkDocument) throws XMLStreamException {
        XMLEvent event;
        event = eventReader.nextEvent();
        if (noarkDocument != null) {
            noarkDocument.setFilename(event.asCharacters().getData());
        }
        return event;
    }

    private XMLEventReader getEventReader(Object payload) throws XMLStreamException {
        String doc = getDoc(payload);
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return xmlInputFactory.createXMLEventReader(new StringReader(doc));
    }

    private static String getDoc(Object payload) {
        String doc;
        if (payload instanceof String) {
            doc = (String) payload;
        } else {
            doc = ((Node) payload).getFirstChild().getTextContent().trim();
        }
        return doc;
    }

    public static String queryJpId(Object payload) {
        try {
            return queryPayload(payload, "/Melding/journpost/jpId");
        } catch (PayloadException e) {
            log.warn("Could not read jpId from payload", e);
            return "";
        }
    }

}

package no.difi.meldingsutveksling.noarkexchange;

import com.google.common.collect.Lists;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
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
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@UtilityClass
public class PayloadUtil {

    private static final String APP_RECEIPT_INDICATOR = "AppReceipt";
    private static final String PAYLOAD_UNKNOWN_TYPE = "Payload is of unknown type cannot determine what type of message it is";

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

    static AppReceiptType getAppReceiptType(Object payload) throws JAXBException {
        final String payloadAsString = payloadAsString(payload);

        StringSource source = new StringSource(payloadAsString);
        JAXBContext jaxbContext = JAXBContext.newInstance("no.difi.meldingsutveksling.noarkexchange.schema");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        JAXBElement<AppReceiptType> r = unmarshaller.unmarshal(source, AppReceiptType.class);
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
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(doc.getBytes(java.nio.charset.Charset.forName("utf-8"))));
            result = expression.evaluate(document);
        } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
            throw new PayloadException("Could not execute query \'"+xpath+"\'  on the payload");
        }
        return result;
    }

    public static List<NoarkDocument> parsePayloadForDocuments(Object payload) throws PayloadException {
        List<NoarkDocument> docs = Lists.newArrayList();

        String doc = getDoc(payload);

        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader;
        try {
            eventReader = xmlInputFactory.createXMLEventReader(new StringReader(doc));
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
                            event = eventReader.nextEvent();
                            if (noarkDocument != null) {
                                noarkDocument.setFilename(event.asCharacters().getData());
                            }
                            break;
                        case "veMimeType":
                            event = eventReader.nextEvent();
                            if (noarkDocument != null) {
                                noarkDocument.setContentType(event.asCharacters().getData());
                            }
                            break;
                        case "dbTittel":
                            event = eventReader.nextEvent();
                            if (noarkDocument != null) {
                                noarkDocument.setTitle(event.asCharacters().getData());
                            }
                            break;
                        case "base64":
                            event = eventReader.nextEvent();
                            if (noarkDocument != null) {
                                noarkDocument.setContent(event.asCharacters().getData().getBytes(StandardCharsets.UTF_8));
                            }
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

package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import org.springframework.util.StringUtils;
import org.springframework.xml.transform.StringSource;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class PayloadUtil {
    public static final String APP_RECEIPT_INDICATOR = "AppReceipt";
    public static final String PAYLOAD_UNKNOWN_TYPE = "Payload is of unknown type cannot determine what type of message it is";

    public static boolean isAppReceipt(Object payload) {
        if(payload instanceof String) {
            return ((String) payload).contains(APP_RECEIPT_INDICATOR);
        }
        else if(payload instanceof Node) {
            final String nodeName = ((Node) payload).getFirstChild().getTextContent();
            return nodeName.contains(APP_RECEIPT_INDICATOR);
        } else {
            throw new RuntimeException(PAYLOAD_UNKNOWN_TYPE);
        }
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

    public static AppReceiptType getAppReceiptType(String payload) throws JAXBException {
        StringSource source = new StringSource(payload);
        JAXBContext jaxbContext = JAXBContext.newInstance(AppReceiptType.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        JAXBElement<AppReceiptType> r = unmarshaller.unmarshal(source, AppReceiptType.class);
        return r.getValue();
    }

}

package no.difi.meldingsutveksling.noarkexchange;

import org.w3c.dom.Node;

public class PayloadUtil {
    public static final String APP_RECEIPT_INDICATOR = "AppReceipt";

    public static boolean isAppReceipt(Object payload) {
        if(payload instanceof String) {
            return ((String) payload).contains(APP_RECEIPT_INDICATOR);
        }
        else if(payload instanceof Node) {
            final String nodeName = ((Node) payload).getFirstChild().getTextContent();
            return nodeName.contains(APP_RECEIPT_INDICATOR);
        } else {
            throw new RuntimeException("Payload is of unknown type cannot determine what type of message it is");
        }

    }
}

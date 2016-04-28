package no.difi.meldingsutveksling.noarkexchange;

public class PayloadUtil {
    public static final String APP_RECEIPT_INDICATOR = "AppReceipt";

    public static boolean isAppReceipt(Object payload) {
        return ((String) payload).contains(APP_RECEIPT_INDICATOR);
    }
}

package no.difi.meldingsutveksling.core;

import no.difi.meldingsutveksling.noarkexchange.receive.PayloadConverter;
import no.difi.meldingsutveksling.noarkexchange.receive.PayloadConverterImpl;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import org.w3c.dom.Node;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BestEduConverter {

    private static final String MESSAGE_TYPE_NAMESPACE = "http://www.arkivverket.no/Noark4-1-WS-WD/types";
    private static final String APPRECEIPT_NAMESPACE = "http://www.arkivverket.no/Noark/Exchange/types";

    private static final PayloadConverter meldingTypeConverter = new PayloadConverterImpl<>(MeldingType.class,
            MESSAGE_TYPE_NAMESPACE, "Melding");
    private static final PayloadConverter appReceiptConverter = new PayloadConverterImpl<>(AppReceiptType.class,
            APPRECEIPT_NAMESPACE, "AppReceipt");

    private BestEduConverter() {
    }

    public static String meldingTypeAsString(MeldingType meldingType) {
        return meldingTypeConverter.marshallToString(meldingType);
    }

    public static String appReceiptAsString(AppReceiptType appReceiptType) {
        return appReceiptConverter.marshallToString(appReceiptType);
    }

    public static MeldingType payloadAsMeldingType(Object payload) {
        return (MeldingType) meldingTypeConverter.unmarshallFrom(payloadBytes(payload));
    }

    public static AppReceiptType payloadAsAppReceipt(Object payload) {
        return (AppReceiptType) appReceiptConverter.unmarshallFrom(payloadBytes(payload));
    }

    private static byte[] payloadBytes(Object payload) {
        if (payload instanceof String) {
            return ((String) payload).getBytes(UTF_8);
        } else {
            return ((Node) payload).getFirstChild().getTextContent().trim().getBytes(UTF_8);
        }
    }
}

package no.difi.meldingsutveksling.core;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.noarkexchange.receive.PayloadConverter;
import no.difi.meldingsutveksling.noarkexchange.receive.PayloadConverterImpl;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import org.w3c.dom.Node;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class EDUCoreConverter {

    private static final String CHARSET_UTF8 = "UTF-8";
    private static final String MESSAGE_TYPE_NAMESPACE = "http://www.arkivverket.no/Noark4-1-WS-WD/types";
    private static final String APPRECEIPT_NAMESPACE = "http://www.arkivverket.no/Noark/Exchange/types";

    private static final PayloadConverter meldingTypeConverter = new PayloadConverterImpl<>(MeldingType.class,
            MESSAGE_TYPE_NAMESPACE, "Melding");
    private static final PayloadConverter appReceiptConverter = new PayloadConverterImpl<>(AppReceiptType.class,
            APPRECEIPT_NAMESPACE, "AppReceipt");

    private static final JAXBContext jaxbContext;
    static {
        try {
            jaxbContext = JAXBContext.newInstance(EDUCore.class);
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    public static byte[] marshallToBytes(EDUCore message) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(new JAXBElement<>(new QName("uri", "local"), EDUCore.class, message), os);
            return os.toByteArray();
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException("Unable to create marshaller for " + EDUCore.class, e);
        }
    }

    public static EDUCore unmarshallFrom(byte[] message) {
        final ByteArrayInputStream is = new ByteArrayInputStream(message);
        Unmarshaller unmarshaller;
        try {
            unmarshaller = jaxbContext.createUnmarshaller();
            StreamSource source = new StreamSource(is);
            return unmarshaller.unmarshal(source, EDUCore.class).getValue();
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException("Unable to create unmarshaller for " + EDUCore.class, e);
        }
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
            return ((String) payload).getBytes();
        } else {
            return ((Node) payload).getFirstChild().getTextContent().trim().getBytes();
        }
    }
}

package no.difi.meldingsutveksling.noarkexchange.receive;

import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

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
            throw new RuntimeException(e);
        }
    }

    public byte[] marshallToBytes(EDUCore message) {
        // Need to marshall payload before marshalling the message, since the payload can have different types
        String marshalledPayload;
        PayloadConverter payloadConverter;
        if (message.getMessageType() == EDUCore.MessageType.EDU) {
            payloadConverter = meldingTypeConverter;
            marshalledPayload = payloadConverter.marshallToString(message.getPayloadAsMeldingType());
        } else {
            payloadConverter = appReceiptConverter;
            marshalledPayload = payloadConverter.marshallToString(message.getPayloadAsAppreceiptType());
        }
        message.setPayload(marshalledPayload);

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(new JAXBElement<>(new QName("uri", "local"), EDUCore.class, message), os);
            message.setPayload(payloadConverter.unmarshallFrom(marshalledPayload.getBytes(CHARSET_UTF8)));
            return os.toByteArray();
        } catch (JAXBException | UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to create marshaller for " + EDUCore.class, e);
        }
    }

    public String meldingTypeAsString(MeldingType meldingType) {
        return meldingTypeConverter.marshallToString(meldingType);
    }

    public String appReceiptAsString(AppReceiptType appReceiptType) {
        return appReceiptConverter.marshallToString(appReceiptType);
    }

    public String payloadAsString(EDUCore message) {
        if (message.getMessageType() == EDUCore.MessageType.EDU) {
            return meldingTypeConverter.marshallToString(message.getPayloadAsMeldingType());
        } else {
            return appReceiptConverter.marshallToString(message.getPayloadAsAppreceiptType());
        }
    }

    public EDUCore unmarshallFrom(byte[] message) {
        final ByteArrayInputStream is = new ByteArrayInputStream(message);
        Unmarshaller unmarshaller;
        try {
            unmarshaller = jaxbContext.createUnmarshaller();
            StreamSource source = new StreamSource(is);
            EDUCore eduCore = unmarshaller.unmarshal(source, EDUCore.class).getValue();
            PayloadConverterImpl payloadConverter;
            if (eduCore.getMessageType() == EDUCore.MessageType.EDU) {
                payloadConverter = new PayloadConverterImpl<>(MeldingType.class);
                eduCore.setPayload(payloadConverter.unmarshallFrom(((String)eduCore.getPayload()).getBytes(CHARSET_UTF8)));
            } else {
                payloadConverter = new PayloadConverterImpl<>(AppReceiptType.class);
                eduCore.setPayload(payloadConverter.unmarshallFrom(((String)eduCore.getPayload()).getBytes(CHARSET_UTF8)));
            }
            return eduCore;
        } catch (JAXBException | UnsupportedEncodingException  e) {
            throw new RuntimeException("Unable to create unmarshaller for " + EDUCore.class, e);
        }
    }
}

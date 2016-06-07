package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.*;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createOkResponse;

/**
 * Strategy for handling appreceipt messages.
 *
 * @author Glenn Bech
 */
class AppReceiptPutMessageStrategy implements PutMessageStrategy {

    private final MessageSender messageSender;

    private static final JAXBContext jaxbContext;

    static {
        try {
            jaxbContext = JAXBContext.newInstance("no.difi.meldingsutveksling.noarkexchange.schema");
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    public AppReceiptPutMessageStrategy(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType request) {
        final PutMessageRequestWrapper wrapper = new PutMessageRequestWrapper(request);
        Audit.info("Received AppReceipt", markerFrom(wrapper));
        try {
             AppReceiptType receipt = PayloadUtil.getAppReceiptType(request.getPayload());
            if (asList("OK", "WARNING", "ERROR").contains(receipt.getType())) {
                wrapper.swapSenderAndReceiver();
                messageSender.sendMessage(wrapper.getRequest());
            }
            if (receipt.getType().equals("OK")) {
                Audit.info("AppReceipt sent to "+ wrapper.getRecieverPartyNumber(), markerFrom(wrapper));
            } else if (asList("ERROR", "WARNING").contains(receipt.getType())) {
                final MessageException me = new MessageException(StatusMessage.APP_RECEIPT_CONTAINS_ERROR);
                Audit.warn(me.getStatusMessage().getTechnicalMessage(), markerFrom(wrapper));
            }
            return createOkResponse();
        } catch (JAXBException e) {
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
                final Marshaller marshaller = jaxbContext.createMarshaller();
                StringWriter requestAsXml = new StringWriter(4096);
                marshaller.marshal(new ObjectFactory().createPutMessageRequest(request), requestAsXml);
                System.out.println(">>> Failing request: " + requestAsXml.toString());
                Audit.error("This request resultet in error: {}", markerFrom(new PutMessageRequestWrapper(request)), requestAsXml.toString());
            } catch (JAXBException e1) {
                throw new MeldingsUtvekslingRuntimeException(e1);
            }
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }
}

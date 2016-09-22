package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.noarkexchange.PayloadUtil;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.core.EDUCoreMarker.markerFrom;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createOkResponse;

/**
 * Strategy for handling appreceipt messages.
 *
 * @author Glenn Bech
 */
class AppReceiptMessageStrategy implements MessageStrategy {

    private final MessageSender messageSender;

    private static final JAXBContext jaxbContext;

    static {
        try {
            jaxbContext = JAXBContext.newInstance("no.difi.meldingsutveksling.noarkexchange.schema");
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    public AppReceiptMessageStrategy(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public PutMessageResponseType putMessage(EDUCore request) {
        Audit.info("Received AppReceipt", markerFrom(request));
        AppReceiptType receipt = request.getPayloadAsAppreceiptType();
        if (asList("OK", "WARNING", "ERROR").contains(receipt.getType())) {
            request.swapSenderAndReceiver();
            messageSender.sendMessage(request);
        }
        if ("OK".equals(receipt.getType())) {
            Audit.info("AppReceipt sent to "+ request.getReceiver().getOrgNr(), markerFrom(request));
        } else if (asList("ERROR", "WARNING").contains(receipt.getType())) {
            final MessageException me = new MessageException(StatusMessage.APP_RECEIPT_CONTAINS_ERROR);
            Audit.warn(me.getStatusMessage().getTechnicalMessage(), markerFrom(request));
        }
        return createOkResponse();
    }


}

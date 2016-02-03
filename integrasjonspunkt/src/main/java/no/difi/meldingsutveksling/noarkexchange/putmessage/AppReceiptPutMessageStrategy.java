package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.xml.transform.StringSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createOkResponse;

/**
 * Strategy for handling appreceipt messages.
 *
 * @author Glenn Bech
 */
class AppReceiptPutMessageStrategy implements PutMessageStrategy {

    private final PutMessageContext context;

    private static final JAXBContext jaxbContext;

    static {
        try {
            jaxbContext = JAXBContext.newInstance("no.difi.meldingsutveksling.noarkexchange.schema");
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    public AppReceiptPutMessageStrategy(PutMessageContext context) {
        this.context = context;
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType request) {
        final PutMessageRequestWrapper wrapper = new PutMessageRequestWrapper(request);
        Audit.info("Received Appreceipt", markerFrom(wrapper));
        final String payload = StringEscapeUtils.unescapeHtml((String) request.getPayload());
        try {
            StringSource source = new StringSource(payload);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<AppReceiptType> r = unmarshaller.unmarshal(source, AppReceiptType.class);
            AppReceiptType receipt = r.getValue();
            if (receipt.getType().equals("OK")) {
                Audit.info("Received Appreceipt OK returning document to sender." + wrapper.getRecieverPartyNumber(), markerFrom(wrapper));
                context.getMessageSender().sendMessage(request);
            }
            return createOkResponse();
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }
}

package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.ProcessState;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.StatusMessageType;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.xml.transform.StringSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import static no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory.createOkResponse;

/**
 * Strategy for handling appreceipt messages.
 *
 * @author Glenn Bech
 */
class AppReceiptPutMessageStrategy implements PutMessageStrategy {

    private final EventLog eventLog;

    public AppReceiptPutMessageStrategy(EventLog eventLog) {
        this.eventLog = eventLog;
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType request) {
        try {
            String payload = StringEscapeUtils.unescapeHtml((String) request.getPayload());
            JAXBContext jaxbContext = JAXBContext.newInstance("no.difi.meldingsutveksling.noarkexchange.schema");
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<AppReceiptType> t = unmarshaller.unmarshal(new StringSource(payload), AppReceiptType.class);
            AppReceiptType receipt = t.getValue();

            for (StatusMessageType sm : receipt.getMessage())
                eventLog.log(new Event(ProcessState.APP_RECEIPT).setMessage(sm.getCode() + ", " + sm.getText()));
            return createOkResponse();

        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }
}

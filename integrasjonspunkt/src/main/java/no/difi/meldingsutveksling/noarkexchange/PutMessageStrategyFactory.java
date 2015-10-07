package no.difi.meldingsutveksling.noarkexchange;

import com.sun.org.apache.xerces.internal.dom.ElementNSImpl;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;

/**
 * Factory clsss for putmessage strategies. Responsible for inspecting a payload and returning an appropriate
 * Strategy implementation
 *
 * @author Glenn Bech
 */

final class PutMessageStrategyFactory {

    public static final String APP_RECEIPT_INDICATOR = "AppReceipt";
    public static final String MESSAGE_INDICATOR = "Melding";

    private PutMessageStrategyFactory() {
    }

    static PutMessageStrategy createStrategy(PutMessageContext context, Object payload) {

        //EPhorte
        if (payload instanceof ElementNSImpl) {
            return new BestEDUPutMessageStrategy(context);
        }

        //P360, AppReceipt or any other NOARK system dispatching as text
        if (!(payload instanceof String)) {
            throw new MeldingsUtvekslingRuntimeException("unknown payload class " + payload);
        }

        // app receipt?
        boolean isAppReceipt = ((String) payload).contains(APP_RECEIPT_INDICATOR);
        boolean isBestEDUMessage = ((String) payload).contains(MESSAGE_INDICATOR);
        if (isAppReceipt) {
            return new AppReceiptPutMessageStrategy(context);
            // is Message
        } else if (isBestEDUMessage) {
            return new BestEDUPutMessageStrategy(context);
        } else
            throw new MeldingsUtvekslingRuntimeException("Unknown String based payload " + payload);
        // is unknown string variant
    }
}

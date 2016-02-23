package no.difi.meldingsutveksling.noarkexchange.putmessage;

import com.sun.org.apache.xerces.internal.dom.ElementNSImpl;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;

/**
 * Factory clsss for putmessage strategies. Responsible for inspecting a payload and returning an appropriate
 * Strategy implementation
 *
 * @author Glenn Bech
 */

public final class PutMessageStrategyFactory {

    public static final String APP_RECEIPT_INDICATOR = "AppReceipt";
    public static final String MESSAGE_INDICATOR = "Melding";

    private PutMessageContext context;

    private PutMessageStrategyFactory(PutMessageContext context) {
        this.context = context;
    }

    public static PutMessageStrategyFactory newInstance(PutMessageContext context) {
        return new PutMessageStrategyFactory(context);
    }

    public PutMessageStrategy create(Object payload) {
        if (isEPhorte(payload)) {
            return new BestEDUPutMessageStrategy(context.getMessageSender());
        }
        if (isUnknown(payload)) {
            throw new MeldingsUtvekslingRuntimeException("unknown payload class " + payload);
        }
        if (isAppReceipt(payload)) {
            return new AppReceiptPutMessageStrategy(context);
        } else if (isBestEDUMessage(payload)) {
            return new BestEDUPutMessageStrategy(context.getMessageSender());
        } else
            throw new MeldingsUtvekslingRuntimeException("Unknown String based payload " + payload);
    }

    private boolean isUnknown(Object payload) {
        return !(payload instanceof String);
    }

    private boolean isEPhorte(Object payload) {
        return payload instanceof ElementNSImpl;
    }

    private boolean isBestEDUMessage(Object payload) {
        return ((String) payload).contains(MESSAGE_INDICATOR);
    }

    private boolean isAppReceipt(Object payload) {
        return ((String) payload).contains(APP_RECEIPT_INDICATOR);
    }
}

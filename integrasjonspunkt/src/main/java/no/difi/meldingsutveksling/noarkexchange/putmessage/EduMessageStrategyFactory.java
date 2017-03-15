package no.difi.meldingsutveksling.noarkexchange.putmessage;

import com.sun.org.apache.xerces.internal.dom.ElementNSImpl;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import org.w3c.dom.Document;

import static no.difi.meldingsutveksling.noarkexchange.PayloadUtil.isAppReceipt;

/**
 * Factory clsss for putmessage strategies. Responsible for inspecting a payload and returning an appropriate
 * Strategy implementation
 *
 * @author Glenn Bech
 */

public final class EduMessageStrategyFactory implements MessageStrategyFactory {


    public static final String MESSAGE_INDICATOR = "Melding";
    private final MessageSender messageSender;
    private final IntegrasjonspunktProperties properties;


    public EduMessageStrategyFactory(MessageSender messageSender, IntegrasjonspunktProperties properties) {
        this.messageSender = messageSender;
        this.properties = properties;
    }

    public static EduMessageStrategyFactory newInstance(MessageSender messageSender, IntegrasjonspunktProperties properties) {
        return new EduMessageStrategyFactory(messageSender, properties);
    }

    public MessageStrategy create(Object payload) {
        if (isAppReceipt(payload)) {
            Audit.info("Messagetype AppReceipt");
            return new AppReceiptMessageStrategy(messageSender, properties);
        }
        if (isMeldingTypePayload(payload)) {
            Audit.info("Messagetype EDU");
            return new BestEDUMessageStrategy(messageSender);
        }
        if (isEPhorte(payload)) {
            Audit.info("Messagetype EDU - CData");
            return new BestEDUMessageStrategy(messageSender);
        }
        if (isUnknown(payload)) {
            Audit.error("Unknown payload class");
            throw new MeldingsUtvekslingRuntimeException("unknown payload class " + payload);
        }
        if (isBestEDUMessage(payload)) {
            Audit.info("Messagetype EDU HtmlEndoced");
            return new BestEDUMessageStrategy(messageSender);
        }
        Audit.error("Unknown payload string");
        throw new MeldingsUtvekslingRuntimeException("Unknown String based payload " + payload);
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPO;
    }

    private boolean isMeldingTypePayload(Object payload) {
        return payload instanceof MeldingType;
    }

    private boolean isUnknown(Object payload) {
        return !(payload instanceof String);
    }

    private boolean isEPhorte(Object payload) {
        return payload instanceof ElementNSImpl || payload instanceof Document;
    }

    private boolean isBestEDUMessage(Object payload) {
        return ((String) payload).contains(MESSAGE_INDICATOR);
    }
}

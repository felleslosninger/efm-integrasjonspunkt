package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import org.w3c.dom.Node;

import java.util.Objects;

import static no.difi.meldingsutveksling.noarkexchange.PayloadUtil.queryJpId;

/**
 * Wrapper for a NOARK JournalpostID. The class will extract The NOARK jpID field for different
 * payload styles
 *
 * @author Glenn Bech
 */
public class JournalpostId {

    private String jpId;

    public JournalpostId(String jpId) {
        this.jpId = jpId;
    }

    public static JournalpostId fromPutMessage(PutMessageRequestWrapper message) {
        if (message.getMessageType() != PutMessageRequestWrapper.MessageType.EDUMESSAGE) {
            return new JournalpostId("");
        }
        return new JournalpostId(queryJpId(message.getPayload()));
    }

    public static JournalpostId fromPayload(Object payload) {
        if (payload instanceof String || payload instanceof Node) {
            return new JournalpostId(queryJpId(payload));
        }
        if (payload instanceof MeldingType) {
            return new JournalpostId(((MeldingType) payload).getJournpost().getJpId());
        }
        return new JournalpostId("");
    }

    public String value() {
        return jpId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JournalpostId that = (JournalpostId) o;

        return Objects.equals(jpId, that.jpId);
    }

    @Override
    public int hashCode() {
        return jpId != null ? jpId.hashCode() : 0;
    }


}

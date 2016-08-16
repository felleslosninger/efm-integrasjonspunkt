package no.difi.meldingsutveksling.noarkexchange;

import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.logging.MarkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.difi.meldingsutveksling.logging.MarkerFactory.journalPostIdMarker;
import static no.difi.meldingsutveksling.noarkexchange.PayloadUtil.isAppReceipt;

public class PutMessageMarker {
    public static final Logger logger = LoggerFactory.getLogger(PutMessageMarker.class);

    /**
     * Creates LogstashMarker with conversation id from the putMessageRequest that will appear
     * in the logs when used.
     *
     * @param requestAdapter request that contains journal post id, receiver party number, sender party number and conversation id
     * @return LogstashMarker
     */
    public static LogstashMarker markerFrom(PutMessageRequestWrapper requestAdapter) {
        final LogstashMarker messageTypeMarker = MarkerFactory.messageTypeMarker(requestAdapter.getMessageType().name().toLowerCase());
        final LogstashMarker receiverMarker = MarkerFactory.receiverMarker(requestAdapter.getRecieverPartyNumber());
        final LogstashMarker senderMarker = MarkerFactory.senderMarker(requestAdapter.getSenderPartynumber());
        final LogstashMarker conversationIdMarker = MarkerFactory.conversationIdMarker(requestAdapter.getConversationId());
        final LogstashMarker markers = conversationIdMarker.and(receiverMarker).and(senderMarker).and(messageTypeMarker);

        if(requestAdapter.hasPayload() && !isAppReceipt(requestAdapter.getPayload())) {
            try {
                return journalPostIdMarker(requestAdapter.getJournalPostId()).and(markers);
            } catch (PayloadException e) {
                logger.error(markers, "We don't want to end execution because of logging problems", e);
            }
        }
        return markers;
    }
}

package no.difi.meldingsutveksling.ptp;

import java.io.InputStream;

/**
 * Object to hold parameters used to send messages to sikker digital post
 */
public interface MeldingsformidlerRequest {
    InputStream getDocument();
    String getMottakerPid();
    String getSubject();

    String getDocumentName();

    String getDocumentTitle();

    String getSenderOrgnumber();

    String getConversationId();

    // === Optional fields ===
    /**
     * Optional - defaults to Norwegian
     * @return
     */
    String getSpraakKode();

    /**
     * Optional?
     * @return
     */
    String getQueueId();
}

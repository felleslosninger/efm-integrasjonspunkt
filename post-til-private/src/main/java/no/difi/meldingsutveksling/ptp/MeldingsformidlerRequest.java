package no.difi.meldingsutveksling.ptp;

import java.io.InputStream;

/**
 * Object to hold parameters used to send messages to sikker digital post
 */
public interface MeldingsformidlerRequest {

    /**
     *
     * @return the actual content of the post (mail)
     */
    InputStream getDocument();

    /**
     *
     * @return recipient person identifier
     */
    String getMottakerPid();

    /**
     *
     * @return subject in the post (mail)
     */
    String getSubject();

    /**
     *
     * @return typically the prefix in the filename
     */
    String getDocumentName();

    /**
     *
     * @return this title will be used to display the post in the target portal
     */
    String getDocumentTitle();

    /**
     *
     * @return virksomhetens organization number
     */
    String getSenderOrgnumber();

    /**
     *
     * @return an ID that uniquely identifies this message conversation. Must be in UIID format
     */
    String getConversationId();

    /**
     *
     * @return postkasse adresse as defined in KRR for the recipient person
     */
    String getPostkasseAdresse();

    /**
     *
     * @return virksomhetssertifikat of the sending virksomhet
     */
    byte[] getCertificate();

    /**
     *
     * @return the organization number of the postkasse provider as defined in KRR
     */
    String getOrgnrPostkasse();

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

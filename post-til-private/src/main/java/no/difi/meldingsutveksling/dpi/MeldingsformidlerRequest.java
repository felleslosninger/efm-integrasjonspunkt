package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.nextmove.PostAddress;
import no.difi.meldingsutveksling.nextmove.PostalCategory;
import no.difi.meldingsutveksling.nextmove.PrintColor;
import no.difi.meldingsutveksling.nextmove.ReturnHandling;
import no.digdir.dpi.client.domain.Document;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Object to hold parameters used to send messages to sikker digital post
 */
public interface MeldingsformidlerRequest {

    /**
     * @return the main document post (mail)
     */
    Document getDocument();


    /**
     * @return the attachements for the post (mail). This is the other documents in the JournalPost
     */
    List<Document> getAttachments();

    StandardBusinessDocumentHeader getStandardBusinessDocumentHeader();

    /**
     * @return recipient person identifier
     */
    String getMottakerPid();

    /**
     * @return subject in the post (mail)
     */
    String getSubject();

    /**
     * @return virksomhetens organization number
     */
    String getSenderOrgnumber();

    /**
     * @return orgnr to send on behalf of
     */
    Optional<String> getOnBehalfOfOrgnr();

    /**
     * @return avsenderIdentifikator
     */
    Optional<String> getAvsenderIdentifikator();

    /**
     * @return fakturaReferanse
     */
    Optional<String> getFakturaReferanse();

    /**
     * @return an ID that uniquely identifies this message conversation. Must be in UIID format
     */
    String getConversationId();

    /**
     * @return postkasse adresse as defined in KRR for the recipient person
     */
    String getPostkasseAdresse();

    /**
     * @return virksomhetssertifikat of the sending virksomhet
     */
    byte[] getCertificate();

    /**
     * @return the organization number of the postkasse provider as defined in KRR
     */
    String getOrgnrPostkasse();

    /**
     * Needed if email notification is enabled
     *
     * @return the email adress of the person(s) to be notified
     */
    String getEmailAddress();

    /**
     * Needed if sms notification is enabled
     *
     * @return text displayed in notification
     */
    String getSmsVarslingstekst();

    /**
     * Needed if e-mail notification is enabled
     *
     * @return text displayed in notification
     */
    String getEmailVarslingstekst();

    /**
     * Needed if sms notification is enabled
     *
     * @return mobile phone number of person(s) to be notified
     */
    String getMobileNumber();

    /**
     * @return true if allowed to send notification(s) regarding the message being sent
     */
    boolean isNotifiable();

    /**
     * The print provider should be used if mailbox cannot be used technically or legally.
     * Service Registry should be able to determine this based on KRR service.
     * <p>
     * The rules are typically: the user has chosen a mailbox, the user has reserved from getting digital mail or the
     * user has become inactive.
     * <p>
     * The major techincal difference between digital post and physical/print is that the latter does not have a
     * postkasseadresse.
     *
     * @return true if DPI print provider should be used instead of the preferred Digital mailbox
     */
    boolean isPrintProvider();

    PostAddress getPostAddress();

    PostAddress getReturnAddress();

    Integer getSecurityLevel();

    Date getVirkningsdato();

    String getLanguage();

    boolean isAapningskvittering();

    PrintColor getPrintColor();

    PostalCategory getPostalCategory();

    ReturnHandling getReturnHandling();
}

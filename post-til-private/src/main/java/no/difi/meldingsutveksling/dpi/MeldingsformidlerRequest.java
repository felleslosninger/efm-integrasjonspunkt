package no.difi.meldingsutveksling.dpi;

import lombok.Builder;
import lombok.Value;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.nextmove.PostAddress;
import no.difi.meldingsutveksling.nextmove.PostalCategory;
import no.difi.meldingsutveksling.nextmove.PrintColor;
import no.difi.meldingsutveksling.nextmove.ReturnHandling;
import no.digdir.dpi.client.domain.Parcel;

import java.time.OffsetDateTime;

@Value
@Builder(builderClassName = "Builder")
public class MeldingsformidlerRequest {

    StandardBusinessDocumentHeader standardBusinessDocumentHeader;
    Parcel parcel;
    String mottakerPid;
    String subject;
    String senderOrgnumber;
    String onBehalfOfOrgnr;
    String avsenderIdentifikator;
    String fakturaReferanse;
    String conversationId;

    /**
     * @return postkasse adresse as defined in KRR for the recipient person
     */
    String postkasseAdresse;

    /**
     * @return virksomhetssertifikat of the sending virksomhet
     */
    byte[] certificate;

    /**
     * @return the organization number of the postkasse provider as defined in KRR
     */
    String orgnrPostkasse;

    /**
     * Needed if email notification is enabled
     *
     * @return the email adress of the person(s) to be notified
     */
    String emailAddress;
    String smsVarslingstekst;
    String emailVarslingstekst;
    String mobileNumber;
    boolean notifiable;

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
    boolean printProvider;
    PostAddress postAddress;
    PostAddress returnAddress;
    Integer securityLevel;
    OffsetDateTime virkningsdato;
    String language;
    boolean aapningskvittering;
    PrintColor printColor;
    PostalCategory postalCategory;
    ReturnHandling returnHandling;
}

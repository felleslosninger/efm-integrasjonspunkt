package no.difi.meldingsutveksling.noarkexchange;

/**
 * Created by mfhoel on 30.11.15.
 */
public enum Status {
    MISSING_RECIEVER_CERTIFICATE("Mottakers sertifikat mangler i adresseregister", "Missing reciever certificate for orgnumber {} in adresseregister"),
    MISSING_SENDER_CERTIFICATE("Avsenders sertifikat mangler i adresseregisteret", "Missing sender certficiate for orgnumber {} in adresseregister"),
    MISSING_RECIEVER_ORGANIZATION_NUMBER("Mottakers organisasjonsnummer mangler", "Message is missing a recipient organization number");
    private final String endUserMessage;
    private final String technicalMessage;

    Status(String endUserMessage, String technicalMessage) {
        this.endUserMessage = endUserMessage;
        this.technicalMessage = technicalMessage;
    }

    public String getEndUserMessage() {
        return endUserMessage;
    }

    public String getTechnicalMessage() {
        return technicalMessage;
    }
}

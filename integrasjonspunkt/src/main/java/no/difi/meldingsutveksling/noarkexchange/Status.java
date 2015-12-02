package no.difi.meldingsutveksling.noarkexchange;

/**
 * Created by mfhoel on 30.11.15.
 */
public enum Status {
    MISSING_RECIEVER_CERTIFICATE("Mottakers sertifikat mangler i adresseregister", "Missing reciever certificate for orgnumber {} in adresseregister", 1),
    MISSING_SENDER_CERTIFICATE("Avsenders sertifikat mangler i adresseregisteret", "Missing sender certficiate for orgnumber {} in adresseregister", 2),
    MISSING_RECIEVER_ORGANIZATION_NUMBER("Mottakers organisasjonsnummer mangler", "Message is missing a recipient organization number", 3);
    private final String endUserMessage;
    private final String technicalMessage;
    private String id;

    Status(String endUserMessage, String technicalMessage, int id) {
        this.endUserMessage = endUserMessage;
        this.technicalMessage = technicalMessage;
        this.id = String.valueOf(id);
    }

    public String getEndUserMessage() {
        return endUserMessage;
    }

    public String getTechnicalMessage() {
        return technicalMessage;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

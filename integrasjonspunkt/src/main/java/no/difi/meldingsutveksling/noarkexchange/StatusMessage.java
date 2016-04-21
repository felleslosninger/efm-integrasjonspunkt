package no.difi.meldingsutveksling.noarkexchange;

public enum StatusMessage {
    MISSING_RECIEVER_CERTIFICATE("Mottakers sertifikat mangler i adresseregister", "Missing reciever certificate for orgnumber {} in adresseregister", 1),
    MISSING_SENDER_CERTIFICATE("Avsenders sertifikat mangler i adresseregisteret", "Missing sender certficiate for orgnumber {} in adresseregister", 2),
    MISSING_RECIEVER_ORGANIZATION_NUMBER("Mottakers organisasjonsnummer mangler", "Message is missing a recipient organization number", 3),
    UNABLE_TO_CREATE_STANDARD_BUSINESS_DOCUMENT("Ikke i stand til å sende melding til mottaker", "Unable to create standard business document that reciever expects", 4),
    UNABLE_TO_EXTRACT_ZIP_CONTENTS("Kan ikke behandle melding", "Unable to extract zip contents", 5),
    UNABLE_TO_EXTRACT_BEST_EDU("Teknisk feil: kan ikke behandle melding", "Unable to extract BEST EDU", 6),
    UNABLE_TO_FIND_RECEIVER("Mottaker ikke funnet", "Motaker ikke funnet i Adresseregister eller på MSH",7),
    MISSING_PAYLOAD("Request has missing or empty payload", "MissingPayload", 8);


    private final String endUserMessage;
    private final String technicalMessage;
    private String id;

    StatusMessage(String endUserMessage, String technicalMessage, int id) {
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

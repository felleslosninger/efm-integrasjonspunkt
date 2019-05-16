package no.difi.meldingsutveksling.mail;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

import java.util.Optional;

public class MailClient implements NoarkClient {

    private final IntegrasjonspunktProperties props;
    private String subject;

    public MailClient(IntegrasjonspunktProperties props) {
        this.props = props;
    }

    public MailClient(IntegrasjonspunktProperties props, String subject) {
        this.props = props;
        this.subject = subject;
    }

    @Override
    public boolean canRecieveMessage(String orgnr) {
        return true;
    }

    @Override
    public PutMessageResponseType sendEduMelding(PutMessageRequestType request) {
        new EduMailSender(props).send(request, getSubject(request));
        return PutMessageResponseFactory.createOkResponse();
    }

    private String getSubject(PutMessageRequestType request) {
        return Optional.ofNullable(subject).orElseGet(() -> getDefaultSubject(request));
    }

    private String getDefaultSubject(PutMessageRequestType request) {
        return String.format("Integrasjonspunkt: melding fra %s, conversationId=%s",
                request.getEnvelope().getSender().getOrgnr(),
                request.getEnvelope().getConversationId());
    }
}

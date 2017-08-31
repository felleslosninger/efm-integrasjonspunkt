package no.difi.meldingsutveksling.mail;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

import java.util.Optional;

public class MailClient implements NoarkClient {

    private IntegrasjonspunktProperties props;
    private Optional<String> subject;

    public MailClient(IntegrasjonspunktProperties props, Optional subject) {
        this.props = props;
        this.subject = subject;
    }

    @Override
    public boolean canRecieveMessage(String orgnr) {
        return true;
    }

    @Override
    public PutMessageResponseType sendEduMelding(PutMessageRequestType request) {
        EduMailSender eduMailSender = new EduMailSender(props);
        String defaultSubject = String.format("Integrasjonspunkt: melding fra %s, conversationId=%s",
                request.getEnvelope().getSender().getOrgnr(), request.getEnvelope().getConversationId());
        eduMailSender.send(request, subject.orElse(defaultSubject));
        return PutMessageResponseFactory.createOkResponse();
    }
}

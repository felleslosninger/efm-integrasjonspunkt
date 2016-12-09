package no.difi.meldingsutveksling.mail;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

public class MailClient implements NoarkClient {

    private IntegrasjonspunktProperties props;

    public MailClient(IntegrasjonspunktProperties props) {
        this.props = props;
    }

    @Override
    public boolean canRecieveMessage(String orgnr) {
        return true;
    }

    @Override
    public PutMessageResponseType sendEduMelding(PutMessageRequestType request) {
        EduMailSender eduMailSender = new EduMailSender(props);
        eduMailSender.send(request, "Integrasjonspunkt: melding fra "+request.getEnvelope().getSender()
                .getOrgnr()+", conversationId="+request.getEnvelope().getConversationId());
        return PutMessageResponseFactory.createOkResponse();
    }
}

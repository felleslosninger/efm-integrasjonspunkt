package no.difi.meldingsutveksling.mail;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory;
import no.difi.meldingsutveksling.noarkexchange.receive.PutMessageRequestConverter;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

public class EduMailClient implements NoarkClient {

    private IntegrasjonspunktProperties props;

    public EduMailClient(IntegrasjonspunktProperties props) {
        this.props = props;
    }

    @Override

    public boolean canRecieveMessage(String orgnr) {
        return true;
    }

    @Override
    public PutMessageResponseType sendEduMelding(PutMessageRequestType request) {
        EduMailSender eduMailSender = new EduMailSender(props);
        PutMessageRequestConverter converter = new PutMessageRequestConverter();
        byte[] requestAsBytes = converter.marshallToBytes(request);
        eduMailSender.send(requestAsBytes, "Integrasjonspunkt: melding fra "+request.getEnvelope().getSender()
                .getOrgnr()+", conversationId="+request.getEnvelope().getConversationId());
        return PutMessageResponseFactory.createOkResponse();
    }
}

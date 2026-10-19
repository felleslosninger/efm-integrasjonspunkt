package no.difi.meldingsutveksling.ks.svarut.ws;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ks.svarut.ForsendelseStatus;
import no.difi.meldingsutveksling.ks.svarut.SvarUtClient;
import no.difi.meldingsutveksling.ks.svarut.SvarUtServiceException;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.move.common.io.pipe.Reject;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
class SvarUtWebServiceClient implements SvarUtClient {

    private final FiksWebServiceMapper fiksMapper;
    private final FiksWebServiceStatusMapper fiksStatusMapper;
    private final SvarUtInternalWebServiceClientHolder svarUtClientHolder;

    @Override
    public String sendMessage(String forsendelsesId, NextMoveOutMessage message, X509Certificate cert, Reject reject) {
        try {
            SendForsendelseMedId forsendelse = fiksMapper.mapFrom(forsendelsesId, message, cert, reject);
            return svarUtClientHolder.getClient(message.getSenderIdentifier()).sendMessage(forsendelse);
        } catch (NextMoveException e) {
            throw new SvarUtServiceException("Couldn't create Forsendelse", e);
        }
    }

    @Override
    public List<ForsendelseStatus> getForsendelseStatuser(String senderOrgnr, Set<String> forsendelseIds) {
        return svarUtClientHolder.getClient(senderOrgnr).getForsendelseStatuser(forsendelseIds)
            .stream()
            .map(p -> new ForsendelseStatus(p.forsendelsesid, fiksStatusMapper.mapFrom(p.forsendelseStatus)))
            .toList();
    }

    @Override
    public String getForsendelseId(Conversation conversation) {
        return svarUtClientHolder.getClient(conversation.getSenderIdentifier())
            .getForsendelseId(conversation.getMessageId());
    }

    @Override
    public List<String> retreiveForsendelseTyper(String senderOrgnr) {
        return svarUtClientHolder.getClient(senderOrgnr).retreiveForsendelseTyper();
    }
}

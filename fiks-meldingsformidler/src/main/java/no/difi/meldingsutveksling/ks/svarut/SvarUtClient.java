package no.difi.meldingsutveksling.ks.svarut;

import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.move.common.io.pipe.Reject;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface SvarUtClient {

    String sendMessage(String forsendelsesId, NextMoveOutMessage message, X509Certificate cert, Reject reject);

    String getForsendelseId(Conversation conversation);

    List<ForsendelseStatus> getForsendelseStatuser(String senderOrgnr, Set<String> forsendelseIds);

    Collection<String> retreiveForsendelseTyper(String senderOrgnr);
}

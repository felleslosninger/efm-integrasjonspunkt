package no.difi.meldingsutveksling.ks.svarut.rest;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ks.svarut.ForsendelseStatus;
import no.difi.meldingsutveksling.ks.svarut.SvarUtClient;
import no.difi.meldingsutveksling.ks.svarut.SvarUtServiceException;
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.move.common.io.pipe.Reject;
import no.ks.svarut.klient.forsendelse.eksternRef.v2.EksternRefKlientV2;
import no.ks.svarut.klient.forsendelse.send.v3.SendKlientV3;
import no.ks.svarut.klient.forsendelse.status.v3.StatusKlientV3;
import no.ks.svarut.klient.forsendelse.typer.v2.TyperKlientV2;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class SvarUtRestClient implements SvarUtClient {

    private final UUID kontoId;
    private final SendKlientV3 sendKlient;
    private final FiksRestMapper fiksMapper;
    private final ClientContext clientContext;
    private final TyperKlientV2 typerKlient;
    private final StatusKlientV3 statusKlient;
    private final EksternRefKlientV2 eksternRefKlient;
    private final FiksRestStatusMapper fiksStatusMapper;

    @Override
    public String sendMessage(String forsendelsesId, NextMoveOutMessage message, X509Certificate cert, Reject reject) {
        Map<@NonNull String, InputStream> dokumentnavnToData = new HashMap<>();

        try {
            for (BusinessMessageFile file : message.getFiles()) {
                dokumentnavnToData.put(file.getFilename(),
                    fiksMapper.getEncryptedDocument(message.getMessageId(), file, cert, reject).getInputStream());
            }

            UUID id = sendKlient.sendTilOrganisasjon(kontoId,
                fiksMapper.getOrganisasjonForsendelse(message),
                dokumentnavnToData);
            return id.toString();
        } catch (NextMoveException e) {
            throw new SvarUtServiceException("Couldn't create Forsendelse", e);
        } catch (IOException e) {
            throw new SvarUtServiceException("Couldn't send message", e);
        } finally {
            for (InputStream is : dokumentnavnToData.values()) {
                try {
                    is.close();
                } catch (IOException ex) {
                    log.warn("Failed to close input stream", ex);
                }
            }
        }
    }

    @Override
    public List<ForsendelseStatus> getForsendelseStatuser(String senderOrgnr, Set<String> forsendelseIds) {
        return clientContext.withSenderOrg(senderOrgnr, () -> statusKlient.hentStatuser(kontoId,
                forsendelseIds.stream().map(fiksMapper::toUUID).toList())
            .stream()
            .map(p -> new ForsendelseStatus(p.getId().toString(), fiksStatusMapper.mapFrom(p)))
            .toList());
    }

    @Override
    public String getForsendelseId(Conversation conversation) {
        return clientContext.withSenderOrg(conversation.getSenderIdentifier(), () ->
            eksternRefKlient.finnForsendelserKnyttetTilEksternRef(kontoId, conversation.getMessageId())
                .stream().findFirst().orElse(null)
        );
    }

    @Override
    public Collection<String> retreiveForsendelseTyper(String senderOrgnr) {
        return typerKlient.hentForsendelsestyper();
    }

}

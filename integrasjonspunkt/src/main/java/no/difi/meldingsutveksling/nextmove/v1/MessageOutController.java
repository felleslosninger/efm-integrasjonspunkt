package no.difi.meldingsutveksling.nextmove.v1;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.InnsynskravMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.PubliseringMessage;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartRequest;

import java.util.UUID;

@RestController
@RequestMapping("/out/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageOutController {

    private final NextMoveMessageService messageService;
    private final SBDFactory sbdFactory;
    private final IntegrasjonspunktProperties properties;

    @PostMapping
    @Transactional
    public ResponseEntity create(@RequestBody NextMoveV1Message message) {
        if (Strings.isNullOrEmpty(message.getConversationId())) {
            message.setConversationId(UUID.randomUUID().toString());
        }

        StandardBusinessDocument sbd;
        if ("DPE_INNSYN".equals(message.getServiceIdentifier())) {
            sbd = sbdFactory.createNextMoveSBD(
                    Iso6523.of(ICD.NO_ORG, properties.getOrg().getNumber()),
                    Iso6523.of(ICD.NO_ORG, message.getReceiverId()),
                    message.getConversationId(),
                    message.getConversationId(),
                    properties.getEinnsyn().getDefaultInnsynskravProcess(),
                    properties.getEinnsyn().getDefaultInnsynskravDocumentType(),
                    new InnsynskravMessage()
                            .setOrgnr(message.getCustomProperties().getOrDefault("orgnumber", properties.getOrg().getNumber()))
                            .setEpost(message.getCustomProperties().getOrDefault("epost", ""))
            );
        } else {
            sbd = sbdFactory.createNextMoveSBD(
                    Iso6523.of(ICD.NO_ORG, properties.getOrg().getNumber()),
                    Iso6523.of(ICD.NO_ORG, message.getReceiverId()),
                    message.getConversationId(),
                    message.getConversationId(),
                    properties.getEinnsyn().getDefaultJournalProcess(),
                    properties.getEinnsyn().getDefaultJournalDocumentType(),
                    new PubliseringMessage().setOrgnr(message.getCustomProperties().getOrDefault("orgnumber", properties.getOrg().getNumber()))
            );
        }
        messageService.createMessage(sbd);
        return ResponseEntity.ok(message);
    }

    @PostMapping("/{conversationId}")
    public ResponseEntity send(@PathVariable("conversationId") String conversationId,
                               MultipartRequest request) {
        NextMoveOutMessage outMessage = messageService.getMessage(conversationId);
        request.getFileMap().values().forEach(f -> messageService.addFile(outMessage, f));
        messageService.sendMessage(outMessage);
        return ResponseEntity.ok().build();
    }
}

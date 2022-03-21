package no.difi.meldingsutveksling.status.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.api.StatusStrategy;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.mapping.FiksStatusMapper;
import no.difi.meldingsutveksling.ks.svarut.ForsendelseIdService;
import no.difi.meldingsutveksling.ks.svarut.SvarUtWebServiceClient;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPF;
import static no.difi.meldingsutveksling.nextmove.ArkivmeldingKvitteringType.OK;
import static no.difi.meldingsutveksling.receipt.ReceiptStatus.LEST;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@Order
public class DpfStatusStrategy implements StatusStrategy {

    private final ConversationService conversationService;
    private final ForsendelseIdService forsendelseIdService;
    private final FiksStatusMapper fiksStatusMapper;
    private final SvarUtWebServiceClient client;
    private final IntegrasjonspunktProperties props;
    private final SBDFactory sbdFactory;
    private final NextMoveQueue nextMoveQueue;

    @Override
    @Transactional
    public void checkStatus(@NotNull Set<Conversation> conversations) {
        updateStatuses(conversations);
    }

    public void updateStatuses(Set<Conversation> conversations) {
        // Check for missing ids first
        Set<Conversation> missingIds = conversations.stream()
                .filter(c -> forsendelseIdService.getForsendelseId(c) == null)
                .peek(c -> conversationService.registerStatus(c, fiksStatusMapper.noForsendelseId()))
                .collect(Collectors.toSet());
        if (!missingIds.isEmpty()) {
            log.warn("Could not find forsendelseId for the following messages: {}", missingIds.stream()
                    .map(Conversation::getMessageId).collect(Collectors.joining(", ")));
        }

        Map<String, Conversation> forsendelseIdMap = conversations.stream()
                .filter(c -> !missingIds.contains(c))
                .collect(Collectors.toMap(forsendelseIdService::getForsendelseId, c -> c));

        if (!forsendelseIdMap.isEmpty()) {
            client.getForsendelseStatuser(props.getFiks().getUt().getEndpointUrl().toString(), forsendelseIdMap.keySet()).forEach(s -> {
                Conversation c = forsendelseIdMap.get(s.getForsendelsesid());
                MessageStatus status = fiksStatusMapper.mapFrom(s.getForsendelseStatus());
                if (!c.hasStatus(status)) {
                    if (ReceiptStatus.valueOf(status.getStatus()) == LEST &&
                            c.getDocumentIdentifier().equals(props.getArkivmelding().getDefaultDocumentType()) &&
                            props.getArkivmelding().isGenerateReceipts()) {
                        nextMoveQueue.enqueueIncomingMessage(sbdFactory.createArkivmeldingReceiptFrom(c, OK), DPF);
                    }
                    conversationService.registerStatus(c, status);
                }

                if (!c.isPollable()) {
                    forsendelseIdService.delete(c.getMessageId());
                }
            });
        }
    }

    @NotNull
    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return DPF;
    }
}

package no.difi.meldingsutveksling.status.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.api.StatusStrategy;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.mapping.FiksStatusMapper;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
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
    private final FiksStatusMapper fiksStatusMapper;
    private final SvarUtService svarUtService;
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
                .filter(c -> svarUtService.getForsendelseId(c) == null)
                .peek(c -> conversationService.registerStatus(c, fiksStatusMapper.noForsendelseId()))
                .collect(Collectors.toSet());
        if (!missingIds.isEmpty()) {
            log.warn("Could not find forsendelseId for the following messages: {}", missingIds.stream()
                    .map(Conversation::getMessageId).collect(Collectors.joining(", ")));
        }

        conversations.stream()
                .filter(c -> !missingIds.contains(c))
                .collect(Collectors.groupingBy(Conversation::getSenderIdentifier, Collectors.toMap(svarUtService::getForsendelseId, c -> c)))
                .forEach((sender, idMap) -> {
                    svarUtService.getForsendelseStatuser(props.getFiks().getUt().getEndpointUrl().toString(), sender, idMap.keySet()).forEach(s -> {
                        Conversation c = idMap.get(s.getForsendelsesid());
                        MessageStatus status = fiksStatusMapper.mapFrom(s.getForsendelseStatus());
                        if (!c.hasStatus(status)) {
                            if (ReceiptStatus.valueOf(status.getStatus()) == LEST &&
                                    c.getDocumentIdentifier().equals(props.getArkivmelding().getDefaultDocumentType()) &&
                                    isNullOrEmpty(props.getNoarkSystem().getType()) &&
                                    props.getArkivmelding().isGenerateReceipts()) {
                                nextMoveQueue.enqueueIncomingMessage(sbdFactory.createArkivmeldingReceiptFrom(c, OK), DPF);
                            }
                            conversationService.registerStatus(c, status);
                        }

                        if (!c.isPollable()) {
                            svarUtService.deleteForsendelseIdByMessageId(c.getMessageId());
                        }
                    });
                });

    }

    @NotNull
    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return DPF;
    }

    @Override
    public boolean isStartPolling(@NotNull MessageStatus status) {
        return ReceiptStatus.SENDT.toString().equals(status.getStatus());
    }

    @Override
    public boolean isStopPolling(@NotNull MessageStatus status) {
        return false;
    }

}

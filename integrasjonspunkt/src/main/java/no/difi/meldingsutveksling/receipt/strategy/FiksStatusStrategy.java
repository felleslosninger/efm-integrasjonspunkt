package no.difi.meldingsutveksling.receipt.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.mapping.FiksStatusMapper;
import no.difi.meldingsutveksling.ks.svarut.ForsendelseIdService;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.ks.svarut.SvarUtWebServiceClient;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.StatusStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
public class FiksStatusStrategy implements StatusStrategy {

    private final ConversationService conversationService;
    private final ForsendelseIdService forsendelseIdService;
    private final FiksStatusMapper fiksStatusMapper;
    private final SvarUtWebServiceClient client;
    private final IntegrasjonspunktProperties props;

    @Override
    @Transactional
    public void checkStatus(Set<Conversation> conversations) {
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
                conversationService.registerStatus(c, fiksStatusMapper.mapFrom(s.getForsendelseStatus()));
                if (!c.isPollable()) {
                    forsendelseIdService.delete(c.getMessageId());
                }
            });
        }
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPF;
    }
}

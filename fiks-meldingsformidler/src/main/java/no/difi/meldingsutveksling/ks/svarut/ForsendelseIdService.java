package no.difi.meldingsutveksling.ks.svarut;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.CacheConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.status.Conversation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.google.common.base.Strings.isNullOrEmpty;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ForsendelseIdService {

    private final ForsendelseIdRepository forsendelseIdRepository;
    private final IntegrasjonspunktProperties props;
    private final SvarUtWebServiceClient client;

    @Cacheable(value = CacheConfig.CACHE_FORSENDELSEID, key = "#conversation.messageId")
    @Transactional(readOnly = true)
    public String getForsendelseId(Conversation conversation) {
        return forsendelseIdRepository.findByMessageId(conversation.getMessageId())
                .map(ForsendelseIdEntry::getForsendelseId)
                .orElseGet(() -> {
                    String id = client.getForsendelseId(getFiksUtUrl(), conversation.getMessageId());
                    newEntry(conversation.getMessageId(), id);
                    return id;
                });
    }

    @Transactional
    public void newEntry(String messageId, String forsendelseId) {
        if (!isNullOrEmpty(forsendelseId)) {
            log.debug("Saving mapping for messageId={} -> forsendelseId={}", messageId, forsendelseId);
            forsendelseIdRepository.save(new ForsendelseIdEntry(messageId, forsendelseId));
        }
    }

    @Transactional
    public void delete(String messageId) {
        forsendelseIdRepository.deleteByMessageId(messageId);
    }

    private String getFiksUtUrl() {
        return props.getFiks().getUt().getEndpointUrl().toString();
    }
}

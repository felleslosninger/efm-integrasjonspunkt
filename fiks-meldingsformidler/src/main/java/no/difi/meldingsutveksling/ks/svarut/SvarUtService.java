package no.difi.meldingsutveksling.ks.svarut;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.CertificateParserException;
import no.difi.meldingsutveksling.config.CacheConfig;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.nextmove.HasSikkerhetsNivaa;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.move.common.io.pipe.PromiseMaker;
import org.springframework.cache.annotation.Cacheable;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;

@Slf4j
@RequiredArgsConstructor
public class SvarUtService {

    private final SvarUtClient svarUtClient;
    private final PromiseMaker promiseMaker;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final ForsendelseIdRepository forsendelseIdRepository;

    public void send(NextMoveOutMessage message) {
        ServiceRecord serviceRecord;
        try {
            Integer sikkerhetsNivaa = (message.getBusinessMessage() instanceof HasSikkerhetsNivaa<?> e) ? e.getSikkerhetsnivaa() : null;
            serviceRecord = serviceRegistryLookup.getServiceRecord(SRParameter.builder(message.getReceiverIdentifier())
                    .securityLevel(sikkerhetsNivaa)
                    .process(message.getSbd().getProcess())
                    .conversationId(message.getConversationId()).build(),
                message.getSbd().getDocumentType());
        } catch (ServiceRegistryLookupException e) {
            throw new SvarUtServiceException("DPF service record not found for identifier=%s".formatted(message.getReceiverIdentifier()), e);
        }

        promiseMaker.promise(reject -> {
            String forsendelseId = getForsendelseId(message);
            svarUtClient.sendMessage(forsendelseId, message, toX509Certificate(serviceRecord.getPemCertificate()), reject);
            saveForsendelseIdMapping(message.getMessageId(), forsendelseId);
            return null;
        }).await();
    }

    private String getForsendelseId(NextMoveOutMessage message) {
        Optional<String> senderRef = SBDUtil.getOptionalSenderRef(message.getSbd());
        // Confirm that SenderRef is a valid UUID, else use messageId
        if (senderRef.isPresent()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                UUID.fromString(senderRef.get());
            } catch (IllegalArgumentException e) {
                senderRef = Optional.empty();
            }
        }

        return senderRef.orElse(message.getMessageId());
    }

    @Cacheable(value = CacheConfig.CACHE_FORSENDELSEID, key = "#conversation.messageId")
    public String getForsendelseId(Conversation conversation) {
        return forsendelseIdRepository.findByMessageId(conversation.getMessageId())
            .map(ForsendelseIdEntry::getForsendelseId)
            .orElseGet(() -> {
                String id = svarUtClient.getForsendelseId(conversation);
                saveForsendelseIdMapping(conversation.getMessageId(), id);
                return id;
            });
    }

    public List<ForsendelseStatus> getForsendelseStatuser(String senderOrgnr, Set<String> forsendelseIds) {
        return svarUtClient.getForsendelseStatuser(senderOrgnr, forsendelseIds);
    }

    public void saveForsendelseIdMapping(String messageId, String forsendelseId) {
        if (!isNullOrEmpty(forsendelseId)) {
            log.debug("Saving mapping for messageId={} -> forsendelseId={}", messageId, forsendelseId);
            forsendelseIdRepository.save(new ForsendelseIdEntry(messageId, forsendelseId));
        }
    }

    public void deleteForsendelseIdByMessageId(String messageId) {
        forsendelseIdRepository.deleteByMessageId(messageId);
    }

    @Cacheable(CacheConfig.SVARUT_FORSENDELSETYPER)
    public Collection<String> retreiveForsendelseTyper(String senderOrgnr) {
        return svarUtClient.retreiveForsendelseTyper(senderOrgnr);
    }

    private X509Certificate toX509Certificate(String pemCertificate) {
        try {
            return CertificateParser.parse(pemCertificate);
        } catch (CertificateParserException e) {
            throw new SvarUtServiceException("Certificate is invalid", e);
        }
    }
}

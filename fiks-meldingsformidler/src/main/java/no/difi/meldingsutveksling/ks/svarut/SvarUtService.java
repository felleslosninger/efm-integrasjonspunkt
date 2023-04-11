package no.difi.meldingsutveksling.ks.svarut;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.CertificateParserException;
import no.difi.meldingsutveksling.config.CacheConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.mapping.FiksMapper;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.move.common.io.pipe.PromiseMaker;
import no.difi.move.common.io.pipe.Reject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;

@Component
@Slf4j
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@RequiredArgsConstructor
public class SvarUtService {

    private final SvarUtClientHolder svarUtClientHolder;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final FiksMapper fiksMapper;
    private final IntegrasjonspunktProperties props;
    private final PromiseMaker promiseMaker;
    private final ForsendelseIdRepository forsendelseIdRepository;

    @Transactional
    public String send(NextMoveOutMessage message) {
        ServiceRecord serviceRecord;
        try {
            serviceRecord = serviceRegistryLookup.getServiceRecord(SRParameter.builder(message.getReceiver())
                            .securityLevel(message.getBusinessMessage().getSikkerhetsnivaa())
                            .process(message.getSbd().getProcess())
                            .conversationId(message.getConversationId()).build(),
                    message.getSbd().getDocumentType());
        } catch (ServiceRegistryLookupException e) {
            throw new SvarUtServiceException(String.format("DPF service record not found for identifier=%s", message.getReceiverIdentifier()), e);
        }

        return promiseMaker.promise(reject -> {
            try {
                SendForsendelseMedId forsendelse = getForsendelse(message, serviceRecord, reject);
                saveForsendelseIdMapping(message.getMessageId(), forsendelse.getForsendelsesid());
                SvarUtRequest svarUtRequest = new SvarUtRequest(getFiksUtUrl(), forsendelse);
                return svarUtClientHolder.getClient(message.getSenderIdentifier()).sendMessage(svarUtRequest);
            } catch (NextMoveException e) {
                throw new NextMoveRuntimeException("Couldn't create Forsendelse", e);
            }
        }).await();
    }

    @Cacheable(value = CacheConfig.CACHE_FORSENDELSEID, key = "#conversation.messageId")
    @Transactional(readOnly = true)
    public String getForsendelseId(Conversation conversation) {
        return forsendelseIdRepository.findByMessageId(conversation.getMessageId())
                .map(ForsendelseIdEntry::getForsendelseId)
                .orElseGet(() -> {
                    String id = svarUtClientHolder.getClient(conversation.getSenderIdentifier()).getForsendelseId(getFiksUtUrl(), conversation.getMessageId());
                    saveForsendelseIdMapping(conversation.getMessageId(), id);
                    return id;
                });
    }

    @Transactional
    public void saveForsendelseIdMapping(String messageId, String forsendelseId) {
        if (!isNullOrEmpty(forsendelseId)) {
            log.debug("Saving mapping for messageId={} -> forsendelseId={}", messageId, forsendelseId);
            forsendelseIdRepository.save(new ForsendelseIdEntry(messageId, forsendelseId));
        }
    }

    @Transactional
    public void deleteForsendelseIdByMessageId(String messageId) {
        forsendelseIdRepository.deleteByMessageId(messageId);
    }


    private String getFiksUtUrl() {
        return props.getFiks().getUt().getEndpointUrl().toString();
    }

    public List<StatusResult> getForsendelseStatuser(String uri, String senderOrgnr, Set<String> forsendelseIds) {
        return svarUtClientHolder.getClient(senderOrgnr).getForsendelseStatuser(uri, forsendelseIds);
    }

    private SendForsendelseMedId getForsendelse(NextMoveOutMessage message, ServiceRecord serviceRecord, Reject reject) throws NextMoveException {
        final X509Certificate x509Certificate = toX509Certificate(serviceRecord.getPemCertificate());
        return fiksMapper.mapFrom(message, x509Certificate, reject);
    }

    @Cacheable(CacheConfig.SVARUT_FORSENDELSETYPER)
    public List<String> retreiveForsendelseTyper(String senderOrgnr) {
        return svarUtClientHolder.getClient(senderOrgnr).retreiveForsendelseTyper(getFiksUtUrl());
    }

    private X509Certificate toX509Certificate(String pemCertificate) {
        try {
            return CertificateParser.parse(pemCertificate);
        } catch (CertificateParserException e) {
            throw new SvarUtServiceException("Certificate is invalid", e);
        }
    }
}

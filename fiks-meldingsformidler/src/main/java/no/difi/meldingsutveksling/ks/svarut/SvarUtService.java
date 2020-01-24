package no.difi.meldingsutveksling.ks.svarut;

import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.CertificateParserException;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.mapping.FiksMapper;
import no.difi.meldingsutveksling.ks.mapping.FiksStatusMapper;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.pipes.Reject;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@RequiredArgsConstructor
public class SvarUtService {

    private final SvarUtWebServiceClient client;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final FiksMapper fiksMapper;
    private final IntegrasjonspunktProperties props;
    private final CertificateParser certificateParser;
    private final FiksStatusMapper fiksStatusMapper;
    private final PromiseMaker promiseMaker;
    private final ForsendelseIdService forsendelseIdService;

    @Transactional
    public String send(NextMoveOutMessage message) throws NextMoveException {
        ServiceRecord serviceRecord;
        try {
            serviceRecord = serviceRegistryLookup.getServiceRecord(SRParameter.builder(message.getReceiverIdentifier())
                            .securityLevel(message.getBusinessMessage().getSikkerhetsnivaa())
                            .process(message.getSbd().getProcess())
                            .conversationId(message.getConversationId()).build(),
                    message.getSbd().getStandard());
        } catch (ServiceRegistryLookupException e) {
            throw new SvarUtServiceException(String.format("DPF service record not found for identifier=%s", message.getReceiverIdentifier()));
        }

        return promiseMaker.promise(reject -> {
            try {
                SendForsendelseMedId forsendelse = getForsendelse(message, serviceRecord, reject);
                forsendelseIdService.newEntry(message.getMessageId(), forsendelse.getForsendelsesid());
                SvarUtRequest svarUtRequest = new SvarUtRequest(getFiksUtUrl(), forsendelse);
                return client.sendMessage(svarUtRequest);
            } catch (NextMoveException e) {
                throw new NextMoveRuntimeException("Couldn't create Forsendelse", e);
            }
        }).await();
    }

    private String getFiksUtUrl() {
        return props.getFiks().getUt().getEndpointUrl().toString();
    }

    private SendForsendelseMedId getForsendelse(NextMoveOutMessage message, ServiceRecord serviceRecord, Reject reject) throws NextMoveException {
        final X509Certificate x509Certificate = toX509Certificate(serviceRecord.getPemCertificate());
        return fiksMapper.mapFrom(message, x509Certificate, reject);
    }

    public Map<Conversation, MessageStatus> getMessageStatuses(Set<Conversation> conversations) {
        // Check for missing ids first
        HashMap<Conversation, MessageStatus> statusMap = Maps.newHashMap();
        conversations.stream()
                .filter(c -> forsendelseIdService.getForsendelseId(c) == null)
                .forEach(c -> statusMap.put(c, fiksStatusMapper.noForsendelseId()));
        if (!statusMap.isEmpty()) {
            log.warn("Could not find forsendelseId for the following messages: {}", statusMap.keySet().stream()
                    .map(Conversation::getMessageId).collect(Collectors.joining(", ")));
        }

        Map<String, Conversation> forsendelseIdMap = conversations.stream()
                .filter(c -> forsendelseIdService.getForsendelseId(c) != null)
                .collect(Collectors.toMap(forsendelseIdService::getForsendelseId, c -> c));

        if (!forsendelseIdMap.isEmpty()) {
            List<StatusResult> statuses = client.getForsendelseStatuser(getFiksUtUrl(), forsendelseIdMap.keySet());
            statuses.forEach(s -> statusMap.put(forsendelseIdMap.get(s.forsendelsesid), fiksStatusMapper.mapFrom(s.forsendelseStatus)));
        }

        return statusMap;
    }

    public MessageStatus getMessageReceipt(String forsendelseId) {
        if (forsendelseId != null) {
            final ForsendelseStatus forsendelseStatus = client.getForsendelseStatus(getFiksUtUrl(), forsendelseId);
            return fiksStatusMapper.mapFrom(forsendelseStatus);
        } else {
            return fiksStatusMapper.noForsendelseId();
        }
    }

    public void retreiveForsendelseTyper() {
        client.retreiveForsendelseTyper(getFiksUtUrl());
    }

    private X509Certificate toX509Certificate(String pemCertificate) {
        try {
            return certificateParser.parse(pemCertificate);
        } catch (CertificateParserException e) {
            throw new SvarUtServiceException("Certificate is invalid", e);
        }
    }
}

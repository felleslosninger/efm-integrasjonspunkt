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
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.pipes.Reject;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.cert.X509Certificate;
import java.util.List;

@Component
@Slf4j
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@RequiredArgsConstructor
public class SvarUtService {

    private final SvarUtWebServiceClient client;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final FiksMapper fiksMapper;
    private final IntegrasjonspunktProperties props;
    private final PromiseMaker promiseMaker;
    private final ForsendelseIdService forsendelseIdService;

    @Transactional
    public String send(NextMoveOutMessage message) throws NextMoveException {
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

    @Cacheable(CacheConfig.SVARUT_FORSENDELSETYPER)
    public List<String> retreiveForsendelseTyper() {
        return client.retreiveForsendelseTyper(getFiksUtUrl());
    }

    private X509Certificate toX509Certificate(String pemCertificate) {
        try {
            return CertificateParser.parse(pemCertificate);
        } catch (CertificateParserException e) {
            throw new SvarUtServiceException("Certificate is invalid", e);
        }
    }
}

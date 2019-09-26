package no.difi.meldingsutveksling.ks.svarut;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.CertificateParserException;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.mapping.FiksMapper;
import no.difi.meldingsutveksling.ks.mapping.FiksStatusMapper;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
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
import java.util.List;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@RequiredArgsConstructor
public class SvarUtService {

    private final SvarUtWebServiceClient client;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final FiksMapper fiksMapper;
    private final IntegrasjonspunktProperties props;
    private final CertificateParser certificateParser;
    private final FiksStatusMapper fiksStatusMapper;

    @Transactional
    public String send(NextMoveOutMessage message) throws NextMoveException {
        ServiceRecord serviceRecord;
        try {
            serviceRecord = serviceRegistryLookup.getServiceRecord(SRParameter.builder(message.getReceiverIdentifier())
                            .securityLevel(message.getBusinessMessage().getSikkerhetsnivaa())
                            .conversationId(message.getConversationId()).build(),
                    message.getServiceIdentifier());
        } catch (ServiceRegistryLookupException e) {
            throw new SvarUtServiceException(String.format("DPF service record not found for identifier=%s", message.getReceiverIdentifier()));
        }

        SvarUtRequest svarUtRequest = new SvarUtRequest(
                getFiksUtUrl(),
                getForsendelse(message, serviceRecord));

        return client.sendMessage(svarUtRequest);
    }

    private String getFiksUtUrl() {
        return props.getFiks().getUt().getEndpointUrl().toString();
    }

    private SendForsendelseMedId getForsendelse(NextMoveOutMessage message, ServiceRecord serviceRecord) throws NextMoveException {
        final X509Certificate x509Certificate = toX509Certificate(serviceRecord.getPemCertificate());
        return fiksMapper.mapFrom(message, x509Certificate);
    }

    public MessageStatus getMessageReceipt(final Conversation conversation) {
        final String forsendelseId = client.getForsendelseId(getFiksUtUrl(), conversation.getMessageId());
        return getMessageReceipt(forsendelseId);
    }

    public MessageStatus getMessageReceipt(String forsendelseId) {
        if (forsendelseId != null) {
            final ForsendelseStatus forsendelseStatus = client.getForsendelseStatus(getFiksUtUrl(), forsendelseId);
            return fiksStatusMapper.mapFrom(forsendelseStatus);
        } else {
            return fiksStatusMapper.noForsendelseId();
        }
    }

    public List<String> retreiveForsendelseTyper() {
        return client.retreiveForsendelseTyper(getFiksUtUrl());
    }

    private X509Certificate toX509Certificate(String pemCertificate) {
        try {
            return certificateParser.parse(pemCertificate);
        } catch (CertificateParserException e) {
            throw new SvarUtServiceException("Certificate is invalid", e);
        }
    }
}

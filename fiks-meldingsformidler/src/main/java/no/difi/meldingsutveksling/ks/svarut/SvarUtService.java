package no.difi.meldingsutveksling.ks.svarut;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.CertificateParserException;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingException;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.ks.mapping.FiksMapper;
import no.difi.meldingsutveksling.ks.mapping.FiksStatusMapper;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;

@RequiredArgsConstructor
public class SvarUtService {
    private final SvarUtWebServiceClient client;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final FiksMapper fiksMapper;
    private final IntegrasjonspunktProperties props;
    private final CertificateParser certificateParser;

    public String send(EDUCore message) {
        ServiceRecord serviceRecord;
        try {
            serviceRecord = serviceRegistryLookup.getServiceRecord(message.getReceiver().getIdentifier(), message.getServiceIdentifier());
        } catch (ServiceRegistryLookupException e) {
            throw new SvarUtServiceException(String.format("DPF service record not found for identifier=%s", message.getReceiver().getIdentifier()));
        }

        final X509Certificate x509Certificate = toX509Certificate(serviceRecord.getPemCertificate());
        final SendForsendelseMedId forsendelse = fiksMapper.mapFrom(message, x509Certificate);
        SvarUtRequest svarUtRequest = new SvarUtRequest(getFiksUtUrl(), forsendelse);
        return client.sendMessage(svarUtRequest);
    }

    public String send(NextMoveMessage message) throws NextMoveException, ArkivmeldingException {
        ServiceRecord serviceRecord;
        try {
            serviceRecord = serviceRegistryLookup.getServiceRecord(message.getReceiverIdentifier(), message.getServiceIdentifier());
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

    private SendForsendelseMedId getForsendelse(NextMoveMessage message, ServiceRecord serviceRecord) throws NextMoveException, ArkivmeldingException {
        final X509Certificate x509Certificate = toX509Certificate(serviceRecord.getPemCertificate());
        return fiksMapper.mapFrom(message, x509Certificate);
    }

    public MessageStatus getMessageReceipt(final Conversation conversation) {
        final String forsendelseId = client.getForsendelseId(getFiksUtUrl(), conversation.getConversationId());
        if (forsendelseId != null) {
            final ForsendelseStatus forsendelseStatus = client.getForsendelseStatus(getFiksUtUrl(), forsendelseId);
            return FiksStatusMapper.mapFrom(forsendelseStatus);
        } else {
            return MessageStatus.of(ReceiptStatus.FEIL, LocalDateTime.now(), "forsendelseId finnes ikke i SvarUt.");
        }
    }

    private X509Certificate toX509Certificate(String pemCertificate) {
        try {
            return certificateParser.parse(pemCertificate);
        } catch (CertificateParserException e) {
            throw new SvarUtServiceException("Certificate is invalid", e);
        }
    }
}

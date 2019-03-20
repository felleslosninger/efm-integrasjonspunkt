package no.difi.meldingsutveksling.ks.svarut;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.CertificateParserException;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingException;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.ks.mapping.FiksMapper;
import no.difi.meldingsutveksling.ks.mapping.FiksStatusMapper;
import no.difi.meldingsutveksling.ks.receipt.DpfReceiptStatus;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
public class SvarUtService {
    private final SvarUtWebServiceClient client;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final FiksMapper fiksMapper;
    private final IntegrasjonspunktProperties props;
    private final CertificateParser certificateParser;

    public String send(EDUCore message) {
        Optional<ServiceRecord> serviceRecord = serviceRegistryLookup.getServiceRecord(message.getReceiver().getIdentifier(), message.getServiceIdentifier());
        if (!serviceRecord.isPresent()) {
            throw new SvarUtServiceException(String.format("No DPF ServiceRecord found for identifier %s", message.getReceiver().getIdentifier()));
        }

        final X509Certificate x509Certificate = toX509Certificate(serviceRecord.get().getPemCertificate());


        final SendForsendelseMedId forsendelse = fiksMapper.mapFrom(message, x509Certificate);
        SvarUtRequest svarUtRequest = new SvarUtRequest(props.getFiks().getUt().getEndpointUrl().toString(), forsendelse);
        return client.sendMessage(svarUtRequest);
    }

    public String send(NextMoveMessage message) throws NextMoveException, ArkivmeldingException {
        Optional<ServiceRecord> serviceRecord = serviceRegistryLookup.getServiceRecord(message.getReceiverIdentifier(), message.getServiceIdentifier());
        if (!serviceRecord.isPresent()) {
            throw new SvarUtServiceException(String.format("No DPF ServiceRecord found for identifier %s", message.getReceiverIdentifier()));
        }

        final X509Certificate x509Certificate = toX509Certificate(serviceRecord.get().getPemCertificate());
        SendForsendelseMedId forsendelse = fiksMapper.mapFrom(message, x509Certificate);
        SvarUtRequest svarUtRequest = new SvarUtRequest(props.getFiks().getUt().getEndpointUrl().toString(), forsendelse);
        return client.sendMessage(svarUtRequest);
    }

    public MessageStatus getMessageReceipt(final Conversation conversation) {
        final String forsendelseId = client.getForsendelseId(props.getFiks().getUt().getEndpointUrl().toString(), conversation.getConversationId());
        if (forsendelseId != null) {
            final ForsendelseStatus forsendelseStatus = client.getForsendelseStatus(props.getFiks().getUt().getEndpointUrl().toString(), forsendelseId);
            final DpfReceiptStatus receiptStatus = FiksStatusMapper.mapFrom(forsendelseStatus);
            return MessageStatus.of(receiptStatus);
        } else {
            return MessageStatus.of(GenericReceiptStatus.FEIL, LocalDateTime.now(), "forsendelseId finnes ikke i SvarUt.");
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

package no.difi.meldingsutveksling.ks.svarut;

import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.CertificateParserException;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.ks.mapping.FiksMapper;
import no.difi.meldingsutveksling.ks.receipt.DpfReceiptStatus;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

import java.security.cert.X509Certificate;
import java.util.Optional;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPF;

public class SvarUtService {
    private SvarUtWebServiceClient client;
    private ServiceRegistryLookup serviceRegistryLookup;
    private FiksMapper fiksMapper;
    private IntegrasjonspunktProperties props;
    CertificateParser certificateParser;

    public SvarUtService(SvarUtWebServiceClient svarUtClient,
                         ServiceRegistryLookup serviceRegistryLookup,
                         FiksMapper fiksMapper,
                         IntegrasjonspunktProperties props) {
        this.client = svarUtClient;
        this.serviceRegistryLookup = serviceRegistryLookup;
        this.fiksMapper = fiksMapper;
        this.props = props;
        certificateParser = new CertificateParser();
    }

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

    public String send(ConversationResource cr) {
        Optional<ServiceRecord> serviceRecord = serviceRegistryLookup.getServiceRecord(cr.getReceiverId(), DPF);
        if (!serviceRecord.isPresent()) {
            throw new SvarUtServiceException(String.format("No DPF ServiceRecord found for identifier %s", cr.getReceiverId()));
        }

        final X509Certificate x509Certificate = toX509Certificate(serviceRecord.get().getPemCertificate());
        final SendForsendelseMedId forsendelse = fiksMapper.mapFrom(cr, x509Certificate);
        SvarUtRequest svarUtRequest = new SvarUtRequest(props.getFiks().getUt().getEndpointUrl().toString(), forsendelse);
        return client.sendMessage(svarUtRequest);
    }

    public MessageStatus getMessageReceipt(final Conversation conversation) {
        final String forsendelseId = client.getForsendelseId(props.getFiks().getUt().getEndpointUrl().toString(), conversation.getConversationId());
        final ForsendelseStatus forsendelseStatus = client.getForsendelseStatus(props.getFiks().getUt().getEndpointUrl().toString(), forsendelseId);
        final DpfReceiptStatus receiptStatus = fiksMapper.mapFrom(forsendelseStatus);
        return MessageStatus.of(receiptStatus);
    }

    private X509Certificate toX509Certificate(String pemCertificate) {
        try {
            return certificateParser.parse(pemCertificate);
        } catch (CertificateParserException e) {
            throw new SvarUtServiceException("Certificate is invalid", e);
        }
    }
}

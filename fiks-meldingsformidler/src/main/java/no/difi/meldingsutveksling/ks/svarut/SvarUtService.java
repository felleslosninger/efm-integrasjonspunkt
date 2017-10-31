package no.difi.meldingsutveksling.ks.svarut;

import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.CertificateParserException;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.ks.mapping.FiksMapper;
import no.difi.meldingsutveksling.ks.receipt.DpfReceiptStatus;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

import java.security.cert.X509Certificate;

public class SvarUtService {
    private SvarUtWebServiceClient client;
    private ServiceRegistryLookup serviceRegistryLookup;
    private FiksMapper fiksMapper;
    CertificateParser certificateParser;

    public SvarUtService(SvarUtWebServiceClient svarUtClient, ServiceRegistryLookup serviceRegistryLookup, FiksMapper fiksMapper) {
        this.client = svarUtClient;
        this.serviceRegistryLookup = serviceRegistryLookup;
        this.fiksMapper = fiksMapper;
        certificateParser = new CertificateParser();
    }

    public String send(EDUCore message) {
        final ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecord(message.getReceiver().getIdentifier());

        final X509Certificate x509Certificate = toX509Certificate(serviceRecord.getPemCertificate());


        final Forsendelse forsendelse = fiksMapper.mapFrom(message, x509Certificate);
        SvarUtRequest svarUtRequest = new SvarUtRequest(serviceRecord.getEndPointURL(), forsendelse);
        return client.sendMessage(svarUtRequest);
    }

    public MessageStatus getMessageReceipt(final Conversation conversation) {
        final ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecord(conversation.getReceiverIdentifier());
        final String forsendelseId = client.getForsendelseId(serviceRecord.getEndPointURL(), conversation.getConversationId());

        final ForsendelseStatus forsendelseStatus = client.getForsendelseStatus(serviceRecord.getEndPointURL(), forsendelseId);
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

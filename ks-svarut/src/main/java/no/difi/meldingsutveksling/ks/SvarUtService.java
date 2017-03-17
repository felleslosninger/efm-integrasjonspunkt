package no.difi.meldingsutveksling.ks;

import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.CertificateParserException;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.ks.mapping.ForsendelseMapper;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

import java.security.cert.X509Certificate;

public class SvarUtService {
    private SvarUtWebServiceClient client;
    private ServiceRegistryLookup serviceRegistryLookup;
    private ForsendelseMapper forsendelseMapper;
    CertificateParser certificateParser;

    public SvarUtService(SvarUtWebServiceClient svarUtClient, ServiceRegistryLookup serviceRegistryLookup, ForsendelseMapper forsendelseMapper) {
        this.client = svarUtClient;
        this.serviceRegistryLookup = serviceRegistryLookup;
        this.forsendelseMapper = forsendelseMapper;
        certificateParser = new CertificateParser();
    }

    public String send(EDUCore message) {
        final ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecord(message.getReceiver().getIdentifier());

        final X509Certificate x509Certificate = toX509Certificate(serviceRecord.getPemCertificate());


        final Forsendelse forsendelse = forsendelseMapper.mapFrom(message, x509Certificate);
        SvarUtRequest svarUtRequest = new SvarUtRequest(serviceRecord.getEndPointURL(), forsendelse);
        return client.sendMessage(svarUtRequest);
    }

    public ForsendelseStatus getMessageReceipt(final Conversation conversation) {
        final ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecord(conversation.getReceiverIdentifier());
        final String forsendelseId = client.getForsendelseId(serviceRecord.getEndPointURL(), conversation.getConversationId());

        final ForsendelseStatus forsendelseStatus = client.getForsendelseStatus(serviceRecord.getEndPointURL(), forsendelseId);
//        MessageReceipt.of(forsendelseStatus.value())

        // TODO: create messageReceipt from forsendelse status
        return forsendelseStatus;
    }

    private X509Certificate toX509Certificate(String pemCertificate) {
        try {
            return certificateParser.parse(pemCertificate);
        } catch (CertificateParserException e) {
            throw new SvarUtServiceException("Certificate is invalid", e);
        }
    }


    public String getForsendelseIdFor(Conversation conversation) {
        return null;
    }
}

package no.difi.meldingsutveksling.ks;

import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.CertificateParserException;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

import java.security.cert.X509Certificate;

public class SvarUtService {
    private SvarUtWebServiceClient client;
    private ServiceRegistryLookup serviceRegistryLookup;
    private EDUCoreConverter messageConverter;
    CertificateParser certificateParser;

    public SvarUtService(SvarUtWebServiceClient svarUtClient, ServiceRegistryLookup serviceRegistryLookup, EDUCoreConverter messageConverter) {
        this.client = svarUtClient;
        this.serviceRegistryLookup = serviceRegistryLookup;
        this.messageConverter = messageConverter;
        certificateParser = new CertificateParser();
    }

    public String send(EDUCore message) {
        final ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecord(message.getReceiver().getIdentifier());

        final X509Certificate x509Certificate = toX509Certificate(serviceRecord.getPemCertificate());

        final Forsendelse forsendelse = messageConverter.convert(message, x509Certificate);
        SvarUtRequest svarUtRequest = new SvarUtRequest(serviceRecord.getEndPointURL(), forsendelse);
        return client.sendMessage(svarUtRequest);
    }

    public void getForsendelseStatus(final Conversation conversation) {
        final ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecord(conversation.getReceiverIdentifier());
        client.getForsendelseStatus(serviceRecord.getEndPointURL(), conversation.getConversationId());
    }

    private X509Certificate toX509Certificate(String pemCertificate) {
        try {
            return certificateParser.parse(pemCertificate);
        } catch (CertificateParserException e) {
            throw new SvarUtServiceException("Certificate is invalid", e);
        }
    }


}

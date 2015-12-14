package no.difi.meldingsutveksling.services;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.StandardBusinessDocumentWrapper;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import no.difi.virksert.client.VirksertClient;
import no.difi.virksert.client.VirksertClientBuilder;
import no.difi.virksert.client.VirksertClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.cert.Certificate;


@Component
public class AdresseregisterVirksert {

    @Autowired
    IntegrasjonspunktConfig configuration;

    private VirksertClient virksertClient;

    public AdresseregisterVirksert() {
    }

    public AdresseregisterVirksert(VirksertClient virksertClient) {
        this.virksertClient = virksertClient;
    }

    @PostConstruct
    public void init() {
        String adresseRegisterEndPointURL = configuration.getAdresseRegisterEndPointURL();
        virksertClient = VirksertClientBuilder.newInstance()
                .setScope("no.difi.virksert.scope.DemoScope")
                .setUri(adresseRegisterEndPointURL).build();
    }

    public IntegrasjonspunktConfig getConfiguration() {
        return configuration;
    }

    public void setConfiguration(IntegrasjonspunktConfig configuration) {
        this.configuration = configuration;
    }

    public void validateCertificates(StandardBusinessDocumentWrapper documentWrapper) throws MessageException {
        try {
            getCertificate(documentWrapper.getReceiverOrgNumber());
        } catch (CertificateException e) {
            throw new MessageException(e, StatusMessage.MISSING_RECIEVER_CERTIFICATE);
        }
        try {
            getCertificate(documentWrapper.getSenderOrgNumber());
        } catch (CertificateException e) {
            throw new MessageException(e, StatusMessage.MISSING_SENDER_CERTIFICATE);
        }
    }

    public Certificate getCertificate(String orgNumber) throws CertificateException {
        try {
            return virksertClient.fetch(orgNumber);
        } catch (VirksertClientException e) {
            throw new CertificateException("Virkcert cannot find valid certificate for " + orgNumber, e);
        }
    }
}

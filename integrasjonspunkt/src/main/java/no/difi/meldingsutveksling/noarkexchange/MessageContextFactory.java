package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.services.Adresseregister;
import org.springframework.stereotype.Component;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;

@Component
public class MessageContextFactory {

    private Adresseregister adresseregister;
    private ServiceRegistryLookup serviceRegistryLookup;

    public MessageContextFactory(Adresseregister adresseregister,
                                 ServiceRegistryLookup srLookup) {
        this.adresseregister = adresseregister;
        this.serviceRegistryLookup = srLookup;
    }

    public MessageContext from(StandardBusinessDocument sbd) throws MessageContextException {
        MessageContext context = new MessageContext();
        context.setAvsender(createAvsender(sbd.getSenderIdentifier()));
        context.setMottaker(createMottaker(sbd));
        return context;
    }

    public MessageContext from(String senderOrgnr, String receiverOrgnr, Certificate certificate) {
        MessageContext context = new MessageContext();
        context.setAvsender(createAvsender(senderOrgnr));
        context.setMottaker(createMottaker(receiverOrgnr, certificate));
        return context;
    }

    private Avsender createAvsender(String identifier) {
        return Avsender.builder(new Organisasjonsnummer(identifier)).build();
    }

    private Mottaker createMottaker(StandardBusinessDocument sbd) throws MessageContextException {
        return Mottaker.builder(new Organisasjonsnummer(sbd.getReceiverIdentifier()), getMottakerCertificate(sbd)).build();
    }

    private Mottaker createMottaker(String identifier, Certificate certificate) {
        return Mottaker.builder(new Organisasjonsnummer(identifier), certificate).build();
    }

    private Certificate getMottakerCertificate(StandardBusinessDocument sbd) throws MessageContextException {
        ServiceRecord serviceRecord;
        try {
            serviceRecord = serviceRegistryLookup.getServiceRecord(SRParameter.builder(sbd.getReceiverIdentifier())
                    .conversationId(sbd.getConversationId()).build(),
                    sbd.getProcess(),
                    sbd.getStandard());
        } catch (ServiceRegistryLookupException e) {
            throw new MessageContextException(StatusMessage.MISSING_SERVICE_RECORD, e);
        }

        try {
            return adresseregister.getCertificate(serviceRecord);
        } catch (CertificateException e) {
            throw new MessageContextException(e, StatusMessage.MISSING_RECIEVER_CERTIFICATE);
        }
    }

}

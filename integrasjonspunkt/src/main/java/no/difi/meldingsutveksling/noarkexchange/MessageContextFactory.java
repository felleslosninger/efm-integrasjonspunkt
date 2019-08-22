package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.MessageInformable;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.services.Adresseregister;
import org.springframework.stereotype.Component;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;

@Component
public class MessageContextFactory {

    private IntegrasjonspunktProperties props;
    private Adresseregister adresseregister;
    private ServiceRegistryLookup serviceRegistryLookup;

    public MessageContextFactory(IntegrasjonspunktProperties props,
                                 Adresseregister adresseregister,
                                 ServiceRegistryLookup srLookup) {
        this.props = props;
        this.adresseregister = adresseregister;
        this.serviceRegistryLookup = srLookup;
    }

    public MessageContext from(MessageInformable message) throws MessageContextException {
        MessageContext context = new MessageContext();
        context.setAvsender(createAvsender(message.getSenderIdentifier()));
        context.setMottaker(createMottaker(message.getReceiverIdentifier(), message.getServiceIdentifier()));
        context.setJpId("");
        context.setConversationId(message.getConversationId());
        return context;
    }

    public MessageContext from(String senderOrgnr, String receiverOrgnr, String conversationId, Certificate certificate) {
        MessageContext context = new MessageContext();
        context.setAvsender(createAvsender(senderOrgnr));
        context.setMottaker(createMottaker(receiverOrgnr, certificate));
        context.setJpId("");
        context.setConversationId(conversationId);
        return context;
    }

    private Avsender createAvsender(String identifier) {
        return Avsender.builder(new Organisasjonsnummer(identifier)).build();
    }

    private Mottaker createMottaker(String identifier, ServiceIdentifier serviceIdentifier) throws MessageContextException {
        return Mottaker.builder(new Organisasjonsnummer(identifier), getCertificate(identifier, serviceIdentifier)).build();
    }

    private Mottaker createMottaker(String identifier, Certificate certificate) {
        return Mottaker.builder(new Organisasjonsnummer(identifier), certificate).build();
    }

    private Certificate getCertificate(String identifier, ServiceIdentifier serviceIdentifier) throws MessageContextException {
        ServiceRecord serviceRecord;
        try {
            serviceRecord = serviceRegistryLookup.getServiceRecord(identifier, serviceIdentifier);
        } catch (ServiceRegistryLookupException e) {
            throw new MessageContextException(StatusMessage.NO_MATCHING_SERVICEIDENTIFIER, e);
        }

        try {
            return adresseregister.getCertificate(serviceRecord);
        } catch (CertificateException e) {
            if (props.getOrg().getNumber().equals(identifier)) {
                throw new MessageContextException(e, StatusMessage.MISSING_SENDER_CERTIFICATE);
            }
            throw new MessageContextException(e, StatusMessage.MISSING_RECIEVER_CERTIFICATE);
        }
    }
}

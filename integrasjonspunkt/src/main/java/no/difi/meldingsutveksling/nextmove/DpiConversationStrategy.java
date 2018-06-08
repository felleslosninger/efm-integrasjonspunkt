package no.difi.meldingsutveksling.nextmove;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.KeystoreProvider;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPI;
import static no.difi.meldingsutveksling.nextmove.logging.ConversationResourceMarkers.markerFrom;

@Component
@Slf4j
public class DpiConversationStrategy implements ConversationStrategy {

    private IntegrasjonspunktProperties props;
    private ServiceRegistryLookup sr;
    private KeystoreProvider keystore;
    private MessagePersister messagePersister;

    @Autowired
    DpiConversationStrategy(IntegrasjonspunktProperties props,
                            ServiceRegistryLookup sr,
                            MessagePersister messagePersister,
                            KeystoreProvider keystore) {
        this.props = props;
        this.sr = sr;
        this.messagePersister = messagePersister;
        this.keystore = keystore;
    }

    @Override
    public void send(ConversationResource conversationResource) throws NextMoveException {

        DpiConversationResource cr = (DpiConversationResource) conversationResource;
        List<ServiceRecord> serviceRecords = sr.getServiceRecords(cr.getReceiver().getReceiverId(), cr.isMandatoryNotification());
        Optional<ServiceRecord> serviceRecord = serviceRecords.stream()
                .filter(r -> DPI == r.getServiceIdentifier())
                .findFirst();

        if (!serviceRecord.isPresent()) {
            List<ServiceIdentifier> acceptableTypes = serviceRecords.stream()
                    .map(ServiceRecord::getServiceIdentifier)
                    .collect(Collectors.toList());
            String errorStr = String.format("Message is of type '%s', but receiver '%s' accepts types '%s'.",
                    DPI, cr.getReceiver().getReceiverId(), acceptableTypes);
            log.error(markerFrom(conversationResource), errorStr);
            throw new NextMoveException(errorStr);
        }

        NextMoveDpiRequest request = new NextMoveDpiRequest(messagePersister, cr, serviceRecord.get());
        MeldingsformidlerClient client = new MeldingsformidlerClient(props.getDpi(), keystore.getKeyStore());
        try {
            client.sendMelding(request);
        } catch (MeldingsformidlerException e) {
            Audit.error("Failed to send message to DPI", markerFrom(cr), e);
            throw new NextMoveException(e);
        }
    }
}

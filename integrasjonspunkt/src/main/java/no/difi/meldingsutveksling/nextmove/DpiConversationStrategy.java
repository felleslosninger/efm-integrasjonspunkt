package no.difi.meldingsutveksling.nextmove;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.KeystoreProvider;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
                            ObjectProvider<MessagePersister> messagePersister,
                            KeystoreProvider keystore) {
        this.props = props;
        this.sr = sr;
        this.messagePersister = messagePersister.getIfUnique();
        this.keystore = keystore;
    }

    @Override
    public void send(ConversationResource conversationResource) throws NextMoveException {

        DpiConversationResource cr = (DpiConversationResource) conversationResource;
        Optional<ServiceRecord> serviceRecord = sr.getServiceRecord(cr.getReceiverId(), cr.getServiceIdentifier());

        NextMoveDpiRequest request = new NextMoveDpiRequest(props, messagePersister, cr,
                serviceRecord.orElseThrow(() -> new NextMoveRuntimeException(String.format("DPI serviceRecord not found for %s", cr.getReceiverId()))));
        MeldingsformidlerClient client = new MeldingsformidlerClient(props.getDpi(), keystore.getKeyStore());
        try {
            client.sendMelding(request);
        } catch (MeldingsformidlerException e) {
            Audit.error("Failed to send message to DPI", markerFrom(cr), e);
            throw new NextMoveException(e);
        }
    }
}

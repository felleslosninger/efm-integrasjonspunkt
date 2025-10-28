package no.difi.meldingsutveksling.ks.fiksio;

import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.nextmove.FiksIoMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.ks.fiks.io.client.FiksIOKlient;
import no.ks.fiks.io.client.model.KontoId;
import no.ks.fiks.io.client.model.MeldingRequest;
import no.ks.fiks.io.client.model.Payload;
import no.ks.fiks.io.client.model.StreamPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = {"difi.move.feature.enableDPFIO"}, havingValue = "true")
public class FiksIoService {

    private static final Logger log = LoggerFactory.getLogger(FiksIoService.class);

    private final FiksIOKlient fiksIoKlient;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final OptionalCryptoMessagePersister persister;
    private final ConversationService conversationService;

    public FiksIoService(FiksIOKlient fiksIoKlient,
                         ServiceRegistryLookup serviceRegistryLookup,
                         OptionalCryptoMessagePersister persister,
                         ConversationService conversationService) {
        this.fiksIoKlient = fiksIoKlient;
        this.serviceRegistryLookup = serviceRegistryLookup;
        this.persister = persister;
        this.conversationService = conversationService;
    }

    public void sendMessage(NextMoveMessage msg) {
        List<Payload> payloads = new ArrayList<>();
        try {
            msg.getFiles().forEach(f -> {
                try {
                    payloads.add(new StreamPayload(persister.read(msg.getMessageId(), f.getIdentifier()).getInputStream(), f.getFilename()));
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to read payload stream", e);
                }
            });
        } catch (RuntimeException e) {
            throw e;
        }
        createRequest(msg, payloads);
    }

    public void createRequest(NextMoveMessage msg, List<Payload> payloads) {
        SRParameter.SRParameterBuilder params = SRParameter.builder(msg.getReceiverIdentifier())
            .process(msg.getSbd().getProcess())
            .conversationId(msg.getConversationId());

        Optional<FiksIoMessage> fiksIoMsg = msg.getBusinessMessage(FiksIoMessage.class);
        Integer sikkerhetsnivaa = fiksIoMsg.map(FiksIoMessage::getSikkerhetsnivaa).orElse(null);
        if (sikkerhetsnivaa != null) {
            params.securityLevel(sikkerhetsnivaa);
        }
        final ServiceRecord serviceRecord;
        try {
            serviceRecord = serviceRegistryLookup.getServiceRecord(params.build(), msg.getSbd().getDocumentType());
        } catch (ServiceRegistryLookupException e) {
            throw new IllegalStateException("Failed to lookup service record", e);
        }

        MeldingRequest request = MeldingRequest.builder()
            .mottakerKontoId(new KontoId(UUID.fromString(serviceRecord.getService().getEndpointUrl())))
            .meldingType(serviceRecord.getProcess())
            .build();
        var sentMessage = fiksIoKlient.send(request, payloads);
        conversationService.registerStatus(msg.getMessageId(), ReceiptStatus.SENDT, ReceiptStatus.MOTTATT, ReceiptStatus.LEVERT);
        log.debug("FiksIO: Sent message with fiksId={}", sentMessage.getMeldingId());
    }

}

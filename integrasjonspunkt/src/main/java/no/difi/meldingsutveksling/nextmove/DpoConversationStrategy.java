package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingException;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.MessageContextException;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPF;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static no.difi.meldingsutveksling.nextmove.NextMoveMessageMarkers.markerFrom;

@Component
@RequiredArgsConstructor
@Slf4j
public class DpoConversationStrategy implements ConversationStrategy {

    private final ServiceRegistryLookup sr;
    private final MessageSender messageSender;
    private final SvarUtService svarUtService;

    @Override
    public void send(ConversationResource cr) throws NextMoveException {
        List<ServiceRecord> serviceRecords = sr.getServiceRecords(cr.getReceiverId());
        Optional<ServiceRecord> serviceRecord = serviceRecords.stream()
                .filter(r -> DPO == r.getServiceIdentifier())
                .findFirst();
        if (!serviceRecord.isPresent()) {
            List<ServiceIdentifier> acceptableTypes = serviceRecords.stream()
                    .map(ServiceRecord::getServiceIdentifier)
                    .collect(Collectors.toList());
            String errorStr = String.format("Message is of type '%s', but receiver '%s' accepts types '%s'.",
                    DPO, cr.getReceiverId(), acceptableTypes);
            log.error(markerFrom(cr), errorStr);
            throw new NextMoveException(errorStr);
        }
        try {
            messageSender.sendMessage(cr);
        } catch (MessageContextException e) {
            log.error("Send message failed.", e);
            throw new NextMoveException(e);
        }
        Audit.info(String.format("Message [id=%s, serviceIdentifier=%s] sent to altinn",
                cr.getConversationId(), cr.getServiceIdentifier()),
                markerFrom(cr));
    }

    @Override
    public void send(NextMoveMessage message) throws NextMoveException {
        List<ServiceRecord> serviceRecords = sr.getServiceRecords(message.getReceiverIdentifier());
        ServiceRecord serviceRecord = serviceRecords.stream()
                .filter(r -> Arrays.asList(DPO, DPF).contains(r.getServiceIdentifier()))
                .findFirst()
                .orElseThrow(() -> {
                    List<ServiceIdentifier> acceptableTypes = serviceRecords.stream()
                            .map(ServiceRecord::getServiceIdentifier)
                            .collect(Collectors.toList());
                    String errorStr = String.format("Message is of type '%s', but receiver '%s' accepts types '%s'.",
                            message.getServiceIdentifier(), message.getReceiverIdentifier(), acceptableTypes);
                    log.error(markerFrom(message), errorStr);
                    return new NextMoveException(errorStr);
                });

        if (DPO == serviceRecord.getServiceIdentifier()) {
            try {
                messageSender.sendMessage(message);
            } catch (MessageContextException e) {
                log.error("Send message failed.", e);
                throw new NextMoveException(e);
            }
        } else {
            try {
                svarUtService.send(message);
            } catch (ArkivmeldingException e) {
                throw new NextMoveException("Error processing arkivmelding", e);
            }
        }

        Audit.info(String.format("Message [id=%s, serviceIdentifier=%s] sent to altinn",
                message.getConversationId(), message.getServiceIdentifier()),
                markerFrom(message));
    }

}

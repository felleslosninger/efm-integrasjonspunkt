package no.difi.meldingsutveksling.noarkexchange.altinn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.*;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.MessageInfo;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.ks.svarinn.Forsendelse;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnService;
import no.difi.meldingsutveksling.kvittering.SBDReceiptFactory;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.NextMoveQueue;
import no.difi.meldingsutveksling.nextmove.NextMoveServiceBus;
import no.difi.meldingsutveksling.nextmove.StatusMessage;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.receipt.*;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.springframework.scheduling.annotation.Scheduled;

import javax.xml.bind.JAXBElement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static no.difi.meldingsutveksling.domain.sbdh.SBDUtil.isNextMove;
import static no.difi.meldingsutveksling.domain.sbdh.SBDUtil.isReceipt;
import static no.difi.meldingsutveksling.domain.sbdh.SBDUtil.isStatus;
import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;

/**
 * MessagePolling periodically checks Altinn Formidlingstjeneste for new messages. If new messages are discovered they are
 * downloaded forwarded to the Archive system.
 */
@Slf4j
@RequiredArgsConstructor
public class MessagePolling {

    private final IntegrasjonspunktProperties properties;
    private final InternalQueue internalQueue;
    private final IntegrasjonspunktNokkel keyInfo;
    private final TransportFactory transportFactory;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final ConversationService conversationService;
    private final NextMoveQueue nextMoveQueue;
    private final NextMoveServiceBus nextMoveServiceBus;
    private final MessagePersister messagePersister;
    private final AltinnWsClientFactory altinnWsClientFactory;
    private final SvarInnService svarInnService;
    private final SvarInnEduCoreForwarder svarInnEduCoreForwarder;
    private final SvarInnNextMoveForwarder svarInnNextMoveForwarder;
    private final ApplicationContextHolder applicationContextHolder;
    private final SBDReceiptFactory sbdReceiptFactory;

    private ServiceRecord serviceRecord;
    private CompletableFuture batchRead;

    @Scheduled(fixedRateString = "${difi.move.nextmove.serviceBus.pollingrate}")
    public void checkForNewNextMoveMessages() {
        if (properties.getNextmove().getServiceBus().isEnable() &&
                !properties.getNextmove().getServiceBus().isBatchRead()) {
            log.debug("Checking for new NextMove messages..");
            nextMoveServiceBus.getAllMessagesRest();
        }
        if (properties.getNextmove().getServiceBus().isEnable() &&
                properties.getNextmove().getServiceBus().isBatchRead()) {
            if (this.batchRead == null || this.batchRead.isDone()) {
                log.debug("Checking for new NextMove messages (batch)..");
                this.batchRead = nextMoveServiceBus.getAllMessagesBatch();
            } else {
                log.debug("Batch still processing..");
            }
        }
    }

    @Scheduled(fixedRateString = "${difi.move.fiks.pollingrate}")
    public void checkForFiksMessages() {

        if (!properties.getFeature().isEnableDPF()) {
            return;
        }

        log.debug("Checking for new FIKS messages");
        Consumer<Forsendelse> forwarder = getSvarInnForwarder();
        svarInnService.getForsendelser().forEach(forwarder);
    }

    private Consumer<Forsendelse> getSvarInnForwarder() {
        if (properties.getNoarkSystem().isEnable()
                && !properties.getNoarkSystem().getEndpointURL().isEmpty()) {
            return svarInnEduCoreForwarder;
        }

        return svarInnNextMoveForwarder;
    }

    @Scheduled(fixedRate = 15000)
    public void checkForNewMessages() {
        if (!properties.getFeature().isEnableDPO()) {
            return;
        }
        log.debug("Checking for new messages");

        if (serviceRecord == null) {
            serviceRecord = serviceRegistryLookup.getServiceRecord(properties.getOrg().getNumber(), DPO)
                    .orElseThrow(() -> new MeldingsUtvekslingRuntimeException(String.format("DPO ServiceRecord not found for %s", properties.getOrg().getNumber())));
        }

        AltinnWsClient client = altinnWsClientFactory.getAltinnWsClient(serviceRecord);

        List<FileReference> fileReferences = client.availableFiles(properties.getOrg().getNumber());

        if (!fileReferences.isEmpty()) {
            log.debug("New message(s) detected");
        }

        for (FileReference reference : fileReferences) {
            try {
                final DownloadRequest request = new DownloadRequest(reference.getValue(), properties.getOrg().getNumber());
                log.debug(format("Downloading message with altinnId=%s", reference.getValue()));
                StandardBusinessDocument sbd = client.download(request, messagePersister);
                Audit.info(format("Downloaded message with id=%s", sbd.getConversationId()), sbd.createLogstashMarkers());

                if (isNextMove(sbd)) {
                    log.debug(format("NextMove message id=%s", sbd.getConversationId()));
                    if (isStatus(sbd)) {
                        StatusMessage status = (StatusMessage) sbd.getAny();
                        DpoReceiptStatus dpoReceiptStatus = DpoReceiptStatus.valueOf(status.getType().toUpperCase());
                        MessageStatus ms = MessageStatus.of(dpoReceiptStatus);
                        conversationService.registerStatus(sbd.getConversationId(), ms);
                    } else {
                        if (properties.getNoarkSystem().isEnable() && !properties.getNoarkSystem().getEndpointURL().isEmpty()) {
                            internalQueue.enqueueNoark(sbd);
                        } else {
                            nextMoveQueue.enqueue(sbd, DPO);
                        }
                    }

                } else {
                    if (isReceipt(sbd)) {
                        JAXBElement<Kvittering> jaxbKvit = (JAXBElement<Kvittering>) sbd.getAny();
                        Audit.info(format("Message id=%s is a receipt", sbd.getConversationId()),
                                sbd.createLogstashMarkers().and(getReceiptTypeMarker(jaxbKvit.getValue())));
                        MessageStatus status = statusFromKvittering(jaxbKvit.getValue());
                        conversationService.registerStatus(sbd.getConversationId(), status);
                    } else {
                        sendReceipt(sbd.getMessageInfo());
                        log.debug(sbd.createLogstashMarkers(), "Delivery receipt sent");
                        Conversation c = conversationService.registerConversation(sbd);
                        internalQueue.enqueueNoark(sbd);
                        conversationService.registerStatus(c, MessageStatus.of(GenericReceiptStatus.INNKOMMENDE_MOTTATT));
                    }
                }

                client.confirmDownload(request);
                log.debug(markerFrom(reference).and(sbd.createLogstashMarkers()), "Message confirmed downloaded");

            } catch (Exception e) {
                log.error(format("Error during Altinn message polling, message altinnId=%s", reference.getValue()), e);
            }
        }
    }

    private LogstashMarker getReceiptTypeMarker(Kvittering kvittering) {
        final String field = "receipt-type";
        if (kvittering.getLevering() != null) {
            return Markers.append(field, "levering");
        }
        if (kvittering.getAapning() != null) {
            return Markers.append(field, "åpning");
        }
        return Markers.append(field, "unkown");
    }

    private MessageStatus statusFromKvittering(Kvittering kvittering) {
        DpoReceiptStatus status = DpoReceiptStatus.of(kvittering);
        LocalDateTime tidspunkt = kvittering.getTidspunkt().toGregorianCalendar().toZonedDateTime().toLocalDateTime();
        return MessageStatus.of(status, tidspunkt);
    }

    private void sendReceipt(MessageInfo messageInfo) {
        StandardBusinessDocument doc = sbdReceiptFactory.createLeveringsKvittering(messageInfo, keyInfo);
        Transport t = transportFactory.createTransport(doc);
        t.send(applicationContextHolder.getApplicationContext(), doc);
    }
}

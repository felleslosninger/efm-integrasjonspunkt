package no.difi.meldingsutveksling.noarkexchange.altinn;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.*;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreFactory;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateSBD;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.MessageInfo;
import no.difi.meldingsutveksling.domain.NextMoveStreamedFile;
import no.difi.meldingsutveksling.domain.StreamedFile;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.exceptions.TimeToLiveException;
import no.difi.meldingsutveksling.ks.svarinn.*;
import no.difi.meldingsutveksling.kvittering.SBDReceiptFactory;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository;
import no.difi.meldingsutveksling.noarkexchange.*;
import no.difi.meldingsutveksling.noarkexchange.logging.PutMessageResponseMarkers;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.receipt.*;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static no.difi.meldingsutveksling.domain.sbdh.SBDUtil.*;
import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;
import static no.difi.meldingsutveksling.receipt.GenericReceiptStatus.INNKOMMENDE_LEVERT;
import static no.difi.meldingsutveksling.receipt.GenericReceiptStatus.INNKOMMENDE_MOTTATT;

/**
 * MessagePolling periodically checks Altinn Formidlingstjeneste for new messages. If new messages are discovered they are
 * downloaded forwarded to the Archive system.
 */
@Slf4j
@RequiredArgsConstructor
public class MessagePolling implements ApplicationContextAware {

    private ApplicationContext context;

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
    private final NoarkClient noarkClient;
    private final NoarkClient mailClient;
    private final MessageContextFactory messageContextFactory;
    private final AsicHandler asicHandler;
    private final NextMoveMessageInRepository messageRepo;
    private final CreateSBD createSBD;

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
        Set<SvarInnMessage> messages = svarInnService.downloadFiles();
        messages.forEach(m -> {
            if (properties.getNoarkSystem().isEnable() && !properties.getNoarkSystem().getEndpointURL().isEmpty()) {
                createAndForwardEduCore(m);
            } else {
                createAndForwardNextMove(m);
            }
        });
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
                    client.confirmDownload(request);
                    StandardBusinessDocumentHeader header = sbd.getStandardBusinessDocumentHeader();
                    if (isExpired(header)) {
                        MessageStatus ms = MessageStatus.of(GenericReceiptStatus.FEIL, LocalDateTime.now(), expectedResponseDateTimeExpiredErrorMessage(header));
                        conversationService.registerStatus(sbd.getConversationId(), ms);
                        throw new TimeToLiveException(header.getExpectedResponseDateTime());
                    }
                    if (properties.getNoarkSystem().isEnable() && !properties.getNoarkSystem().getEndpointURL().isEmpty()) {
                        internalQueue.enqueueNoark(sbd);
                    } else {
                        nextMoveQueue.enqueue(sbd);
                    }
                    continue;
                }

                if (!isReceipt(sbd)) {
                    sendReceipt(sbd.getMessageInfo());
                    log.debug(sbd.createLogstashMarkers(), "Delivery receipt sent");
                    Conversation c = conversationService.registerConversation(sbd);
                    internalQueue.enqueueNoark(sbd);
                    conversationService.registerStatus(c, MessageStatus.of(GenericReceiptStatus.INNKOMMENDE_MOTTATT));
                }

                client.confirmDownload(request);
                log.debug(markerFrom(reference).and(sbd.createLogstashMarkers()), "Message confirmed downloaded");

                if (isReceipt(sbd)) {
                    JAXBElement<Kvittering> jaxbKvit = (JAXBElement<Kvittering>) sbd.getAny();
                    Audit.info(format("Message id=%s is a receipt", sbd.getConversationId()),
                            sbd.createLogstashMarkers().and(getReceiptTypeMarker(jaxbKvit.getValue())));
                    MessageStatus status = statusFromKvittering(jaxbKvit.getValue());
                    conversationService.registerStatus(sbd.getConversationId(), status);
                }
            } catch (Exception e) {
                log.error(format("Error during Altinn message polling, message altinnId=%s", reference.getValue()), e);
            }
        }
    }

    private void createAndForwardNextMove(SvarInnMessage message) {
        MessageContext context;
        try {
            context = messageContextFactory.from(message.getForsendelse().getSvarSendesTil().getOrgnr(),
                    message.getForsendelse().getMottaker().getOrgnr(),
                    message.getForsendelse().getId(),
                    keyInfo.getX509Certificate());
        } catch (MessageContextException e) {
            log.error("Could not create message context", e);
            return;
        }
        StandardBusinessDocument sbd = createSBD.createNextMoveSBD(
                context.getAvsender().getOrgNummer(),
                context.getMottaker().getOrgNummer(),
                context.getConversationId(),
                ServiceIdentifier.DPO,
                new DpoMessage());
        NextMoveInMessage nextMoveMessage = NextMoveInMessage.of(sbd);

        Arkivmelding arkivmelding = message.toArkivmelding();
        byte[] arkivmeldingBytes;
        try {
            arkivmeldingBytes = ArkivmeldingUtil.marshalArkivmelding(arkivmelding);
        } catch (JAXBException e) {
            log.error("Error marshalling arkivmelding", e);
            return;
        }

        List<StreamedFile> files = Lists.newArrayList();
        ByteArrayInputStream arkivmeldingStream = new ByteArrayInputStream(arkivmeldingBytes);
        files.add(new NextMoveStreamedFile(NextMoveConsts.ARKIVMELDING_FILE, arkivmeldingStream, MediaType.APPLICATION_XML_VALUE));
        message.getSvarInnFiles().forEach(f -> files.add(new NextMoveStreamedFile(f.getFilnavn(),
                new ByteArrayInputStream(f.getContents()),
                f.getMediaType().toString())));

        InputStream asicStream = asicHandler.archiveAndEncryptAttachments(files, context, ServiceIdentifier.DPO);
        try {
            messagePersister.writeStream(nextMoveMessage.getConversationId(), NextMoveConsts.ASIC_FILE, asicStream, -1);
        } catch (IOException e) {
            log.error("Error writing ASiC", e);
        }

        messageRepo.save(nextMoveMessage);
        svarInnService.confirmMessage(message.getForsendelse().getId());
    }

    private void createAndForwardEduCore(SvarInnMessage message) {
        Forsendelse forsendelse = message.getForsendelse();
        List<SvarInnFile> files = message.getSvarInnFiles();
        final EDUCore eduCore = message.toEduCore();

        Conversation c = conversationService.registerConversation(eduCore);
        c = conversationService.registerStatus(c, MessageStatus.of(INNKOMMENDE_MOTTATT));

        PutMessageRequestType putMessage = EDUCoreFactory.createPutMessageFromCore(eduCore);
        if (!validateRequiredFields(forsendelse, eduCore, files)) {
            checkAndSendMail(putMessage, forsendelse.getId());
            return;
        }

        final PutMessageResponseType response = noarkClient.sendEduMelding(putMessage);
        if ("OK".equals(response.getResult().getType())) {
            Audit.info("Message successfully forwarded");
            conversationService.registerStatus(c, MessageStatus.of(INNKOMMENDE_LEVERT));
            svarInnService.confirmMessage(forsendelse.getId());
        } else if ("WARNING".equals(response.getResult().getType())) {
            Audit.info(format("Archive system responded with warning for message with fiks-id %s",
                    forsendelse.getId()), PutMessageResponseMarkers.markerFrom(response));
            conversationService.registerStatus(c, MessageStatus.of(INNKOMMENDE_LEVERT));
            svarInnService.confirmMessage(forsendelse.getId());
        } else {
            Audit.error(format("Message with fiks-id %s failed", forsendelse.getId()), PutMessageResponseMarkers.markerFrom(response));
            checkAndSendMail(putMessage, forsendelse.getId());
        }
    }

    private void checkAndSendMail(PutMessageRequestType message, String fiksId) {
        if (properties.getFiks().getInn().isMailOnError()) {
            Audit.info(format("Sending message with id=%s by mail", fiksId));
            mailClient.sendEduMelding(message);
            svarInnService.confirmMessage(fiksId);
        }
    }

    private boolean validateRequiredFields(Forsendelse forsendelse, EDUCore eduCore, List<SvarInnFile> files) {
        SvarInnFieldValidator validator = SvarInnFieldValidator.validator()
                .addField(forsendelse.getMottaker().getOrgnr(), "receiver: orgnr")
                .addField(eduCore.getSender().getIdentifier(), "sender: orgnr")
                .addField(forsendelse.getSvarSendesTil().getNavn(), "sender: name");
        files.forEach(f ->
                validator.addField(f.getMediaType().toString(), "veDokformat") // veDokformat
                        .addField(f.getFilnavn(), "dbTittel") // dbTittel
        );

        if (!validator.getMissing().isEmpty()) {
            String missingFields = validator.getMissing().stream().reduce((a, b) -> a + ", " + b).get();
            Audit.error(format("Message with id=%s has the following missing field(s): %s",
                    forsendelse.getId(), missingFields));
            return false;
        }

        return true;
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
        StandardBusinessDocument doc = SBDReceiptFactory.createLeveringsKvittering(messageInfo, keyInfo);
        Transport t = transportFactory.createTransport(doc);
        t.send(context, doc);
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        this.context = ac;
    }
}

package no.difi.meldingsutveksling.altinnv3.dpv;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.arkivverket.standarder.noark5.arkivmelding.Journalpost;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.digdir.altinn3.correspondence.model.*;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class CorrespondenceFactory {

    private final NotificationFactory notificationFactory;
    private final Clock clock;
    private final Helper helper;
    private final IntegrasjonspunktProperties props;
    private final ArkivmeldingUtil arkivmeldingUtil;

    @SneakyThrows
    public InitializeCorrespondencesExt create(NextMoveOutMessage message, List<UUID> existingAttachments, List<BusinessMessageFile> newAttachmentsMetaData) {
        if (message.getBusinessMessage() instanceof ArkivmeldingMessage) {
            return handleArkivmeldingMessage(message, existingAttachments, newAttachmentsMetaData);
        }

        if (message.getBusinessMessage() instanceof DigitalDpvMessage) {
            return handleDigitalDpvMessage(message, existingAttachments, newAttachmentsMetaData);
        }

        throw new NextMoveRuntimeException("StandardBusinessDocument.any not instance of %s or %s, aborting".formatted(
            ArkivmeldingMessage.class.getName(), DigitalDpvMessage.class.getName()));
    }

    private InitializeCorrespondencesExt handleDigitalDpvMessage(NextMoveOutMessage message, List<UUID> existingAttachments, List<BusinessMessageFile> newAttachmentsMetaData) {
        DigitalDpvMessage msg = (DigitalDpvMessage) message.getBusinessMessage();

        return create(message,
            msg.getTittel(),
            msg.getSammendrag(),
            msg.getInnhold(),
            existingAttachments,
            newAttachmentsMetaData
        );
    }

    private InitializeCorrespondencesExt handleArkivmeldingMessage(NextMoveOutMessage message, List<UUID> existingAttachments, List<BusinessMessageFile> newAttachmentsMetaData) {
        Map<String, BusinessMessageFile> fileMap = message.getFiles().stream()
            .collect(Collectors.toMap(BusinessMessageFile::getFilename, p -> p));

        Arkivmelding arkivmelding = helper.getArkivmelding(message, fileMap);
        Journalpost jp = arkivmeldingUtil.getJournalpost(arkivmelding);

        return create(message,
            jp.getOffentligTittel(),
            jp.getOffentligTittel(),
            jp.getTittel(),
            existingAttachments,
            newAttachmentsMetaData
        );
    }

    public InitializeCorrespondencesExt create(NextMoveOutMessage message,
                                               String messageTitle,
                                               String messageSummary,
                                               String messageBody,
                                               List<UUID> existingAttachments,
                                               List<BusinessMessageFile> newAttachmentsMetaData) {


        InitializeCorrespondencesExt correspondencesExt = new InitializeCorrespondencesExt();

        correspondencesExt.setCorrespondence(getBaseCorrespondence(message, messageTitle, messageSummary, messageBody, newAttachmentsMetaData));
        correspondencesExt.setRecipients(List.of("urn:altinn:organization:identifier-no:" + message.getReceiverIdentifier()));
        if(existingAttachments != null && !existingAttachments.isEmpty()) correspondencesExt.setExistingAttachments(existingAttachments);

        return correspondencesExt;
    }

    private BaseCorrespondenceExt getBaseCorrespondence(NextMoveOutMessage message,
                                                        String messageTitle,
                                                        String messageSummary,
                                                        String messageBody,
                                                        List<BusinessMessageFile> newAttachmentsMetaData) {

        BaseCorrespondenceExt correspondence = new BaseCorrespondenceExt();

        correspondence.setContent(getContent(messageTitle, messageSummary, messageBody, newAttachmentsMetaData));
        correspondence.setNotification(notificationFactory.getNotification(message));
//        correspondence.setAllowSystemDeleteAfter(getAllowSystemDeleteAfter());
        correspondence.setResourceId(getResourceId(message));
        correspondence.setRequestedPublishTime(OffsetDateTime.now(clock));
        correspondence.setMessageSender(helper.getSenderName(message));
        correspondence.setDueDateTime(getDueDateTime(message));
        correspondence.setSender(message.getSender().getIdentifier());
        correspondence.setIsConfirmationNeeded(false);
        correspondence.setSendersReference(message.getMessageId());

        return correspondence;
    }

    private OffsetDateTime getDueDateTime(NextMoveOutMessage message) {
        Optional<Integer> daysToReply = helper.getDpvSettings(message)
            .flatMap(s -> s.getDagerTilSvarfrist() != null ? Optional.of(s.getDagerTilSvarfrist()) : Optional.empty());


        if (daysToReply.isPresent()) {
            if (daysToReply.get() > 0) {
                return OffsetDateTime.now(clock).plusDays(daysToReply.get());
            }
        } else {
            if (props.getDpv().isEnableDueDate()) {
                return OffsetDateTime.now(clock).plusDays(getDaysToReply());
            }
        }
        return null;
    }

    private Long getDaysToReply() {
        return Optional.ofNullable(props.getDpv().getDaysToReply()).orElse(7L);
    }

    private InitializeCorrespondenceContentExt getContent(String messageTitle, String messageSummary, String messageBody, List<BusinessMessageFile> newAttachmentsMetaData) {
        InitializeCorrespondenceContentExt content = new InitializeCorrespondenceContentExt();

        content.setLanguage("nb");
        content.setMessageTitle(messageTitle);
        content.setMessageSummary(messageSummary);
        content.setMessageBody(messageBody);
        if(newAttachmentsMetaData != null && !newAttachmentsMetaData.isEmpty()) content.setAttachments(getAttachments(newAttachmentsMetaData));

        return content;
    }

    private List<InitializeCorrespondenceAttachmentExt> getAttachments(List<BusinessMessageFile> newAttachmentsMetaData) {
         return newAttachmentsMetaData.stream()
             .map(file -> createInitializeAttachmentExt(file.getFilename(), file.getTitle()))
             .collect(Collectors.toList());
    }

    private InitializeCorrespondenceAttachmentExt createInitializeAttachmentExt(String filename, String title){
        var attachment = new InitializeCorrespondenceAttachmentExt();

        attachment.setFileName(filename);
        attachment.setDisplayName(title);
        attachment.setIsEncrypted(false);
        attachment.setSendersReference("AttachmentReference_as123452");
        //attachment.setDataLocationType(InitializeAttachmentDataLocationTypeExt.NEW_CORRESPONDENCE_ATTACHMENT);

        return attachment;
    }

    private String getResourceId(NextMoveOutMessage message) {
        ServiceRecord serviceRecord = helper.getServiceRecord(message);

        return serviceRecord.getService().getResource();
    }

    private OffsetDateTime getAllowSystemDeleteAfter() {
        return OffsetDateTime.now(clock).plusMinutes(5);
    }
}

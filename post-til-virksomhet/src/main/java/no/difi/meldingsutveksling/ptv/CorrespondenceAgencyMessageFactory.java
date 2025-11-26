package no.difi.meldingsutveksling.ptv;

import com.microsoft.schemas._2003._10.serialization.arrays.ArrayOfstring;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType;
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.AttachmentsV2;
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ExternalContentV2;
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.MyInsertCorrespondenceV2;
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ObjectFactory;
import no.altinn.schemas.services.serviceengine.notification._2009._10.*;
import no.altinn.schemas.services.serviceengine.subscription._2009._10.AttachmentFunctionType;
import no.altinn.services.serviceengine.correspondence._2009._10.CorrespondenceStatusHistoryRequest;
import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.altinn.services.serviceengine.reporteeelementlist._2010._10.BinaryAttachmentExternalBEV2List;
import no.altinn.services.serviceengine.reporteeelementlist._2010._10.BinaryAttachmentV2;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.arkivverket.standarder.noark5.arkivmelding.Journalpost;
import no.difi.meldingsutveksling.DateTimeUtil;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.move.common.io.ResourceDataSource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import jakarta.activation.DataHandler;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.NextMoveConsts.ARKIVMELDING_FILE;

/**
 * Class used to create an InsertCorrespondenceV2 object based on an internal message format.
 */
@Component
@RequiredArgsConstructor
public class CorrespondenceAgencyMessageFactory {

    private final CorrespondenceAgencyConfiguration config;
    private final IntegrasjonspunktProperties properties;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final OptionalCryptoMessagePersister optionalCryptoMessagePersister;
    private final Clock clock;
    private final ReporteeFactory reporteeFactory;
    private final ArkivmeldingUtil arkivmeldingUtil;
    private final ObjectFactory objectFactory = new ObjectFactory();
    private final no.altinn.services.serviceengine.correspondence._2009._10.ObjectFactory correspondenceObjectFactory = new no.altinn.services.serviceengine.correspondence._2009._10.ObjectFactory();
    private final no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory notificationObjectFactory = new no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory();

    @SneakyThrows
    public InsertCorrespondenceV2 create(NextMoveOutMessage message) {
        if (message.getBusinessMessage() instanceof ArkivmeldingMessageAsAttachment) {
            return handleArkivmeldingMessage(message);
        }

        if (message.getBusinessMessage() instanceof DigitalDpvMessageAsAttachment) {
            return handleDigitalDpvMessage(message);
        }

        throw new NextMoveRuntimeException("StandardBusinessDocument.any not instance of %s or %s, aborting".formatted(
                ArkivmeldingMessageAsAttachment.class.getName(), DigitalDpvMessageAsAttachment.class.getName()));
    }

    private InsertCorrespondenceV2 handleDigitalDpvMessage(NextMoveOutMessage message) {
        DigitalDpvMessageAsAttachment msg = (DigitalDpvMessageAsAttachment) message.getBusinessMessage();

        BinaryAttachmentExternalBEV2List attachmentExternalBEV2List = new BinaryAttachmentExternalBEV2List();
        attachmentExternalBEV2List.getBinaryAttachmentV2().addAll(getAttachments(message.getMessageId(), message.getFiles()));

        return create(message,
                msg.getTittel(),
                msg.getSammendrag(),
                msg.getInnhold(),
                attachmentExternalBEV2List);
    }

    private InsertCorrespondenceV2 handleArkivmeldingMessage(NextMoveOutMessage message) {
        Map<String, BusinessMessageFile> fileMap = message.getFiles().stream()
                .collect(Collectors.toMap(BusinessMessageFile::getFilename, p -> p));

        Arkivmelding arkivmelding = getArkivmelding(message, fileMap);
        Journalpost jp = arkivmeldingUtil.getJournalpost(arkivmelding);

        BinaryAttachmentExternalBEV2List attachmentExternalBEV2List = new BinaryAttachmentExternalBEV2List();

        List<BusinessMessageFile> files = arkivmeldingUtil.getFilenames(arkivmelding)
                .stream()
                .map(fileMap::get)
                .filter(Objects::nonNull).collect(Collectors.toList());

        attachmentExternalBEV2List.getBinaryAttachmentV2().addAll(getAttachments(message.getMessageId(), files));

        return create(message,
                jp.getOffentligTittel(),
                jp.getOffentligTittel(),
                jp.getTittel(),
                attachmentExternalBEV2List);
    }

    private Arkivmelding getArkivmelding(NextMoveOutMessage message, Map<String, BusinessMessageFile> fileMap) {
        BusinessMessageFile arkivmeldingFile = Optional.ofNullable(fileMap.get(ARKIVMELDING_FILE))
                .orElseThrow(() -> new NextMoveRuntimeException("%s not found for message %s".formatted(ARKIVMELDING_FILE, message.getMessageId())));

        try {
            Resource resource = optionalCryptoMessagePersister.read(message.getMessageId(), arkivmeldingFile.getIdentifier());
            return arkivmeldingUtil.unmarshalArkivmelding(resource);
        } catch (JAXBException | IOException e) {
            throw new NextMoveRuntimeException("Failed to get Arkivmelding", e);
        }
    }

    private List<BinaryAttachmentV2> getAttachments(String messageId, Collection<BusinessMessageFile> files) {
        return files
                .stream()
                .sorted(Comparator.comparing(BusinessMessageFile::getDokumentnummer))
                .map(file -> getBinaryAttachmentV2(messageId, file))
                .collect(Collectors.toList());
    }

    private BinaryAttachmentV2 getBinaryAttachmentV2(String messageId, BusinessMessageFile file) {
        Resource resource = getResource(messageId, file);
        BinaryAttachmentV2 binaryAttachmentV2 = new BinaryAttachmentV2();
        binaryAttachmentV2.setFunctionType(AttachmentFunctionType.fromValue("Unspecified"));
        binaryAttachmentV2.setFileName(reporteeFactory.createBinaryAttachmentV2FileName(file.getFilename()));
        binaryAttachmentV2.setName(reporteeFactory.createBinaryAttachmentV2Name(file.getTitle()));
        binaryAttachmentV2.setEncrypted(false);
        binaryAttachmentV2.setSendersReference(reporteeFactory.createBinaryAttachmentV2SendersReference("AttachmentReference_as123452"));
        binaryAttachmentV2.setData(reporteeFactory.createBinaryAttachmentV2Data(new DataHandler(new ResourceDataSource(resource))));
        return binaryAttachmentV2;
    }

    private Resource getResource(String messageId, BusinessMessageFile f) {
        try {
            return optionalCryptoMessagePersister.read(messageId, f.getIdentifier());
        } catch (IOException e) {
            throw new NextMoveRuntimeException("Could read file named '%s' for messageId=%s".formatted(
                    f.getIdentifier(), f.getFilename()), e);
        }
    }

    public InsertCorrespondenceV2 create(NextMoveOutMessage message,
                                         String messageTitle,
                                         String messageSummary,
                                         String messageBody,
                                         BinaryAttachmentExternalBEV2List attachments) {

        final InsertCorrespondenceV2 myInsertCorrespondenceV2 = correspondenceObjectFactory.createInsertCorrespondenceV2();
        myInsertCorrespondenceV2.setCorrespondence(getMyInsertCorrespondenceV2(message, messageTitle, messageSummary, messageBody, attachments));
        myInsertCorrespondenceV2.setSystemUserCode(config.getSystemUserCode());
        // Reference set by the message sender
        myInsertCorrespondenceV2.setExternalShipmentReference(message.getMessageId());

        return myInsertCorrespondenceV2;
    }

    private MyInsertCorrespondenceV2 getMyInsertCorrespondenceV2(NextMoveOutMessage message, String messageTitle, String messageSummary, String messageBody, BinaryAttachmentExternalBEV2List attachments) {
        ServiceRecord serviceRecord = getServiceRecord(message);

        MyInsertCorrespondenceV2 correspondence = new MyInsertCorrespondenceV2();
        correspondence.setReportee(objectFactory.createMyInsertCorrespondenceV2Reportee(message.getReceiverIdentifier()));
        // Service code from service record, default 4255
        correspondence.setServiceCode(objectFactory.createMyInsertCorrespondenceV2ServiceCode(serviceRecord.getService().getServiceCode()));
        // Service edition from service record, default 10 (Administration)
        correspondence.setServiceEdition(objectFactory.createMyInsertCorrespondenceV2ServiceEdition(serviceRecord.getService().getServiceEditionCode()));
        // Should the user be allowed to forward the message from portal
        correspondence.setAllowForwarding(objectFactory.createMyInsertCorrespondenceV2AllowForwarding(config.isAllowForwarding()));
        correspondence.setMessageSender(objectFactory.createMyInsertCorrespondenceV2MessageSender(getSenderName(message)));
        // The date and time the message should be visible in the Portal
        correspondence.setVisibleDateTime(DateTimeUtil.toXMLGregorianCalendar(OffsetDateTime.now(clock)));

        Optional<Integer> daysToReply = getDpvSettings(message)
                .flatMap(s -> s.getDagerTilSvarfrist() != null ? Optional.of(s.getDagerTilSvarfrist()) : Optional.empty());
        if (daysToReply.isPresent()) {
            if (daysToReply.get() > 0) {
                correspondence.setDueDateTime(DateTimeUtil.toXMLGregorianCalendar(OffsetDateTime.now(clock).plusDays(daysToReply.get())));
            }
        } else {
            if (properties.getDpv().isEnableDueDate()) {
                correspondence.setDueDateTime(DateTimeUtil.toXMLGregorianCalendar(OffsetDateTime.now(clock).plusDays(getDaysToReply())));
            }
        }

        // The date and time the message can be deleted by the user
        correspondence.setAllowSystemDeleteDateTime(
                objectFactory.createMyInsertCorrespondenceV2AllowSystemDeleteDateTime(
                        DateTimeUtil.toXMLGregorianCalendar(getAllowSystemDeleteDateTime())));

        correspondence.setContent(objectFactory.createMyInsertCorrespondenceV2Content(getExternalContentV2(messageTitle, messageSummary, messageBody, attachments)));
        correspondence.setNotifications(objectFactory.createMyInsertCorrespondenceV2Notifications(getNotificationBEList(message)));
        return correspondence;
    }

    private NotificationBEList getNotificationBEList(NextMoveOutMessage msg) {
        NotificationBEList notifications = new NotificationBEList();
        List<Notification2009> notificationList = createNotifications(msg);
        List<Notification2009> notification = notifications.getNotification();
        notification.addAll(notificationList);
        return notifications;
    }

    private ExternalContentV2 getExternalContentV2(String messageTitle, String messageSummary, String messageBody, BinaryAttachmentExternalBEV2List attachments) {
        ExternalContentV2 externalContentV2 = new ExternalContentV2();
        externalContentV2.setLanguageCode(objectFactory.createExternalContentV2LanguageCode("1044"));
        externalContentV2.setMessageTitle(objectFactory.createExternalContentV2MessageTitle(messageTitle));
        externalContentV2.setMessageSummary(objectFactory.createExternalContentV2MessageSummary(messageSummary));
        externalContentV2.setMessageBody(objectFactory.createExternalContentV2MessageBody(messageBody));
        externalContentV2.setAttachments(objectFactory.createExternalContentV2Attachments(getAttachmentsV2(attachments)));
        return externalContentV2;
    }

    private AttachmentsV2 getAttachmentsV2(BinaryAttachmentExternalBEV2List attachments) {
        AttachmentsV2 attachmentsV2 = new AttachmentsV2();
        attachmentsV2.setBinaryAttachments(objectFactory.createAttachmentsV2BinaryAttachments(attachments));
        return attachmentsV2;
    }

    private ServiceRecord getServiceRecord(NextMoveOutMessage message) {
        ServiceRecord serviceRecord;
        try {
            serviceRecord = serviceRegistryLookup.getServiceRecord(
                    SRParameter.builder(message.getReceiverIdentifier())
                            .conversationId(message.getConversationId())
                            .process(message.getSbd().getProcess())
                            .build(),
                    message.getSbd().getDocumentType());
        } catch (ServiceRegistryLookupException e) {
            throw new MeldingsUtvekslingRuntimeException("Could not get service record for receiver %s".formatted(message.getReceiverIdentifier()), e);
        }
        return serviceRecord;
    }

    private Long getDaysToReply() {
        return Optional.ofNullable(properties.getDpv().getDaysToReply()).orElse(7L);
    }

    private List<Notification2009> createNotifications(NextMoveOutMessage msg) {
        EnumMap<DpvVarselTransportType, TransportType> transportMap = new EnumMap<>(DpvVarselTransportType.class);
        transportMap.put(DpvVarselTransportType.EPOST, TransportType.EMAIL);
        transportMap.put(DpvVarselTransportType.SMS, TransportType.SMS);
        transportMap.put(DpvVarselTransportType.EPOSTOGSMS, TransportType.BOTH);

        TransportType transportType = getDpvSettings(msg)
                .flatMap(s -> s.getVarselTransportType() != null ? Optional.of(s.getVarselTransportType()) : Optional.empty())
                .map(transportMap::get)
                .orElseGet(() -> {
                    if (config.isNotifyEmail() && config.isNotifySms()) {
                        return TransportType.BOTH;
                    } else if (config.isNotifySms()) {
                        return TransportType.SMS;
                    } else if (config.isNotifyEmail()) {
                        return TransportType.EMAIL;
                    } else {
                        return TransportType.BOTH;
                    }
                });

        List<Notification2009> notifications = new ArrayList<>();
        notifications.add(createNotification(msg, transportType));
        return notifications;
    }

    private Notification2009 createNotification(NextMoveOutMessage msg, TransportType type) {

        Notification2009 notification = new Notification2009();
        no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory notificationFactory = new no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory();
        notification.setFromAddress(notificationFactory.createNotification2009FromAddress("no-reply@altinn.no"));
        // The date and time the notification should be sent
        notification.setShipmentDateTime(DateTimeUtil.toXMLGregorianCalendar(OffsetDateTime.now(clock).plusMinutes(5)));
        // Language code of the notification
        notification.setLanguageCode(notificationFactory.createNotification2009LanguageCode("1044"));
        // Notification type
        notification.setNotificationType(notificationFactory.createNotification2009NotificationType(getVarselType(msg)));

        notification.setTextTokens(notificationFactory.createNotification2009TextTokens(createTokens(msg)));
        JAXBElement<ReceiverEndPointBEList> receiverEndpoints = createReceiverEndPoint(type);
        notification.setReceiverEndPoints(receiverEndpoints);

        return notification;
    }

    private String getVarselType(NextMoveOutMessage msg) {
        return getDpvSettings(msg).flatMap(s -> s.getVarselType() != null ? Optional.of(s.getVarselType()) : Optional.empty())
                .orElse(DpvVarselType.VARSEL_DPV_MED_REVARSEL)
                .getFullname();
    }

    private Optional<DpvSettings> getDpvSettings(NextMoveOutMessage msg) {
        if (msg.getBusinessMessage() instanceof ArkivmeldingMessageAsAttachment) {
            ArkivmeldingMessageAsAttachment amMsg = (ArkivmeldingMessageAsAttachment) msg.getBusinessMessage();
            if (amMsg.getDpv() != null) {
                return Optional.of(amMsg.getDpv());
            }
        }
        if (msg.getBusinessMessage() instanceof DigitalDpvMessageAsAttachment) {
            DigitalDpvMessageAsAttachment ddMsg = (DigitalDpvMessageAsAttachment) msg.getBusinessMessage();
            if (ddMsg.getDpv() != null) {
                return Optional.of(ddMsg.getDpv());
            }
        }
        return Optional.empty();
    }

    private TextTokenSubstitutionBEList createTokens(NextMoveOutMessage msg) {
        TextTokenSubstitutionBEList tokens = new TextTokenSubstitutionBEList();
        tokens.getTextToken().add(createTextToken(getNotificationText(msg)));
        return tokens;
    }

    private String getNotificationText(NextMoveOutMessage msg) {
        ServiceRecord serviceRecord = getServiceRecord(msg);
        if (config.getSensitiveServiceCode().equals(serviceRecord.getService().getServiceCode())) {
            return getDpvSettings(msg).flatMap(s -> !isNullOrEmpty(s.getTaushetsbelagtVarselTekst()) ? Optional.of(s.getTaushetsbelagtVarselTekst()) : Optional.empty())
                    .orElse(config.getSensitiveNotificationText())
                    .replace("$reporterName$", getSenderName(msg));
        }
        return getDpvSettings(msg).flatMap(s -> !isNullOrEmpty(s.getVarselTekst()) ? Optional.of(s.getVarselTekst()) : Optional.empty())
                .orElse(config.getNotificationText())
                .replace("$reporterName$", getSenderName(msg));
    }

    private String getSenderName(NextMoveOutMessage msg) {
        String orgnr = SBDUtil.getPartIdentifier(msg.getSbd())
                .map(Iso6523::getOrganizationIdentifier)
                .orElse(properties.getOrg().getNumber());
        return serviceRegistryLookup.getInfoRecord(orgnr).getOrganizationName();
    }

    private OffsetDateTime getAllowSystemDeleteDateTime() {
        return OffsetDateTime.now(clock).plusMinutes(5);
    }

    public CorrespondenceStatusHistoryRequest createReceiptRequest(Set<Conversation> conversations) {
        no.altinn.services.serviceengine.correspondence._2009._10.ObjectFactory of = new no.altinn.services
                .serviceengine.correspondence._2009._10.ObjectFactory();
        CorrespondenceStatusHistoryRequest historyRequest = of.createCorrespondenceStatusHistoryRequest();

        com.microsoft.schemas._2003._10.serialization.arrays.ObjectFactory arrayOf = new com.microsoft.schemas._2003._10.serialization.arrays.ObjectFactory();
        ArrayOfstring arrayOfstring = arrayOf.createArrayOfstring();

        conversations.stream()
                .map(Conversation::getMessageId)
                .forEach(id -> arrayOfstring.getString().add(id));

        JAXBElement<ArrayOfstring> strings = of.createCorrespondenceStatusHistoryRequestCorrespondenceSendersReferences(arrayOfstring);
        historyRequest.setCorrespondenceSendersReferences(strings);
        return historyRequest;
    }

    private TextToken createTextToken(String value) {
        TextToken textToken = new TextToken();
        textToken.setTokenNum(1);
        textToken.setTokenValue(notificationObjectFactory.createTextTokenTokenValue(value));

        return textToken;
    }

    private JAXBElement<ReceiverEndPointBEList> createReceiverEndPoint(TransportType type) {
        ReceiverEndPoint receiverEndPoint = new ReceiverEndPoint();
        receiverEndPoint.setTransportType(notificationObjectFactory.createReceiverEndPointTransportType(type));
        ReceiverEndPointBEList receiverEndpoints = new ReceiverEndPointBEList();
        receiverEndpoints.getReceiverEndPoint().add(receiverEndPoint);
        return notificationObjectFactory.createNotification2009ReceiverEndPoints(receiverEndpoints);
    }
}

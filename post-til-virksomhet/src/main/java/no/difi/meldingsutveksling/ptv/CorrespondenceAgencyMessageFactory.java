package no.difi.meldingsutveksling.ptv;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType;
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.AttachmentsV2;
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ExternalContentV2;
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.MyInsertCorrespondenceV2;
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ObjectFactory;
import no.altinn.schemas.services.serviceengine.correspondence._2014._10.CorrespondenceStatusFilterV2;
import no.altinn.schemas.services.serviceengine.notification._2009._10.*;
import no.altinn.schemas.services.serviceengine.subscription._2009._10.AttachmentFunctionType;
import no.altinn.services.serviceengine.correspondence._2009._10.GetCorrespondenceStatusDetailsV2;
import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.altinn.services.serviceengine.reporteeelementlist._2010._10.BinaryAttachmentExternalBEV2List;
import no.altinn.services.serviceengine.reporteeelementlist._2010._10.BinaryAttachmentV2;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.arkivverket.standarder.noark5.arkivmelding.Journalpost;
import no.difi.meldingsutveksling.DateTimeUtil;
import no.difi.meldingsutveksling.InputStreamDataSource;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.nextmove.message.FileEntryStream;
import no.difi.meldingsutveksling.nextmove.message.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.pipes.Reject;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.activation.DataHandler;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    public InsertCorrespondenceV2 create(NextMoveOutMessage message, Reject reject) {
        if (message.getBusinessMessage() instanceof ArkivmeldingMessage) {
            return handleArkivmeldingMessage(message, reject);
        }

        if (message.getBusinessMessage() instanceof DigitalDpvMessage) {
            return handleDigitalDpvMessage(message, reject);
        }

        throw new NextMoveRuntimeException(String.format("StandardBusinessDocument.any not instance of %s or %s, aborting",
                ArkivmeldingMessage.class.getName(), DigitalDpvMessage.class.getName()));
    }

    private InsertCorrespondenceV2 handleDigitalDpvMessage(NextMoveOutMessage message, Reject reject) {
        DigitalDpvMessage msg = (DigitalDpvMessage) message.getBusinessMessage();

        BinaryAttachmentExternalBEV2List attachmentExternalBEV2List = new BinaryAttachmentExternalBEV2List();
        attachmentExternalBEV2List.getBinaryAttachmentV2().addAll(getAttachments(message.getMessageId(), message.getFiles(), reject));

        return create(message,
                msg.getTittel(),
                msg.getSammendrag(),
                msg.getInnhold(),
                attachmentExternalBEV2List);
    }

    private InsertCorrespondenceV2 handleArkivmeldingMessage(NextMoveOutMessage message, Reject reject) {
        Map<String, BusinessMessageFile> fileMap = message.getFiles().stream()
                .collect(Collectors.toMap(BusinessMessageFile::getFilename, p -> p));

        Arkivmelding arkivmelding = getArkivmelding(message, fileMap);
        Journalpost jp = arkivmeldingUtil.getJournalpost(arkivmelding);

        BinaryAttachmentExternalBEV2List attachmentExternalBEV2List = new BinaryAttachmentExternalBEV2List();

        List<BusinessMessageFile> files = Lists.newArrayList(arkivmeldingUtil.getFilenames(arkivmelding)
                .stream()
                .map(fileMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

        attachmentExternalBEV2List.getBinaryAttachmentV2().addAll(getAttachments(message.getMessageId(), files, reject));

        return create(message,
                jp.getOffentligTittel(),
                jp.getOffentligTittel(),
                jp.getTittel(),
                attachmentExternalBEV2List);
    }

    private Arkivmelding getArkivmelding(NextMoveOutMessage message, Map<String, BusinessMessageFile> fileMap) {
        BusinessMessageFile arkivmeldingFile = Optional.ofNullable(fileMap.get(ARKIVMELDING_FILE))
                .orElseThrow(() -> new NextMoveRuntimeException(String.format("%s not found for message %s", ARKIVMELDING_FILE, message.getMessageId())));


        try (InputStream is = new ByteArrayInputStream(optionalCryptoMessagePersister.read(message.getMessageId(), arkivmeldingFile.getIdentifier()))) {
            return arkivmeldingUtil.unmarshalArkivmelding(is);
        } catch (JAXBException | IOException e) {
            throw new NextMoveRuntimeException("Failed to get Arkivmelding", e);
        }
    }

    private List<BinaryAttachmentV2> getAttachments(String messageId, Collection<BusinessMessageFile> files, Reject reject) {
        return files
                .stream()
                .sorted(Comparator.comparing(BusinessMessageFile::getDokumentnummer))
                .map(file -> getBinaryAttachmentV2(messageId, file, reject))
                .collect(Collectors.toList());
    }

    private BinaryAttachmentV2 getBinaryAttachmentV2(String messageId, BusinessMessageFile file, Reject reject) {
        FileEntryStream fileEntry = getFileEntry(messageId, file, reject);
        BinaryAttachmentV2 binaryAttachmentV2 = new BinaryAttachmentV2();
        binaryAttachmentV2.setFunctionType(AttachmentFunctionType.fromValue("Unspecified"));
        binaryAttachmentV2.setFileName(reporteeFactory.createBinaryAttachmentV2FileName(file.getFilename()));
        binaryAttachmentV2.setName(reporteeFactory.createBinaryAttachmentV2Name(file.getTitle()));
        binaryAttachmentV2.setEncrypted(false);
        binaryAttachmentV2.setSendersReference(reporteeFactory.createBinaryAttachmentV2SendersReference("AttachmentReference_as123452"));
        binaryAttachmentV2.setData(reporteeFactory.createBinaryAttachmentV2Data(new DataHandler(InputStreamDataSource.of(fileEntry.getInputStream()))));
        return binaryAttachmentV2;
    }

    private FileEntryStream getFileEntry(String messageId, BusinessMessageFile f, Reject reject) {
        return optionalCryptoMessagePersister.readStream(messageId, f.getIdentifier(), reject);
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
        // Name of the message sender, always "Avsender"
        correspondence.setMessageSender(objectFactory.createMyInsertCorrespondenceV2MessageSender(getSender()));
        // The date and time the message should be visible in the Portal
        correspondence.setVisibleDateTime(DateTimeUtil.toXMLGregorianCalendar(OffsetDateTime.now(clock)));
        correspondence.setDueDateTime(DateTimeUtil.toXMLGregorianCalendar(OffsetDateTime.now(clock).plusDays(getDaysToReply())));

        // The date and time the message can be deleted by the user
        correspondence.setAllowSystemDeleteDateTime(
                objectFactory.createMyInsertCorrespondenceV2AllowSystemDeleteDateTime(
                        DateTimeUtil.toXMLGregorianCalendar(getAllowSystemDeleteDateTime())));

        correspondence.setContent(objectFactory.createMyInsertCorrespondenceV2Content(getExternalContentV2(messageTitle, messageSummary, messageBody, attachments)));
        correspondence.setNotifications(objectFactory.createMyInsertCorrespondenceV2Notifications(getNotificationBEList()));
        return correspondence;
    }

    private NotificationBEList getNotificationBEList() {
        NotificationBEList notifications = new NotificationBEList();
        List<Notification2009> notificationList = createNotifications();
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
                    message.getSbd().getStandard());
        } catch (ServiceRegistryLookupException e) {
            throw new MeldingsUtvekslingRuntimeException(String.format("Could not get service record for receiver %s", message.getReceiverIdentifier()));
        }
        return serviceRecord;
    }

    private Long getDaysToReply() {
        return Optional.ofNullable(properties.getDpv().getDaysToReply()).orElse(7L);
    }

    private List<Notification2009> createNotifications() {

        List<Notification2009> notifications = new ArrayList<>();

        if (config.isNotifyEmail() && config.isNotifySms()) {
            notifications.add(createNotification(TransportType.BOTH));
        } else if (config.isNotifySms()) {
            notifications.add(createNotification(TransportType.SMS));
        } else if (config.isNotifyEmail()) {
            notifications.add(createNotification(TransportType.EMAIL));
        }

        return notifications;
    }

    private Notification2009 createNotification(TransportType type) {

        Notification2009 notification = new Notification2009();
        no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory notificationFactory = new no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory();
        notification.setFromAddress(notificationFactory.createNotification2009FromAddress("no-reply@altinn.no"));
        // The date and time the notification should be sent
        notification.setShipmentDateTime(DateTimeUtil.toXMLGregorianCalendar(OffsetDateTime.now(clock).plusMinutes(5)));
        // Language code of the notification
        notification.setLanguageCode(notificationFactory.createNotification2009LanguageCode("1044"));
        // Notification type
        notification.setNotificationType(notificationFactory.createNotification2009NotificationType("VarselDPVMedRevarsel"));
        notification.setTextTokens(notificationFactory.createNotification2009TextTokens(createTokens()));
        JAXBElement<ReceiverEndPointBEList> receiverEndpoints = createReceiverEndPoint(type);
        notification.setReceiverEndPoints(receiverEndpoints);

        return notification;
    }

    private TextTokenSubstitutionBEList createTokens() {

        TextTokenSubstitutionBEList tokens = new TextTokenSubstitutionBEList();
        if (StringUtils.hasText(config.getNotificationText())) {
            tokens.getTextToken().add(createTextToken(1, config.getNotificationText()));
        } else {
            tokens.getTextToken().add(createTextToken(1, String.format("$reporteeName$: Du har mottatt en melding fra %s.", getSender())));
        }

        return tokens;
    }

    private String getSender() {
        InfoRecord infoRecord = serviceRegistryLookup.getInfoRecord(properties.getOrg().getNumber());
        return infoRecord.getOrganizationName();
    }

    private OffsetDateTime getAllowSystemDeleteDateTime() {
        return OffsetDateTime.now(clock).plusMinutes(5);
    }

    public GetCorrespondenceStatusDetailsV2 createReceiptRequest(Conversation conversation) {

        no.altinn.services.serviceengine.correspondence._2009._10.ObjectFactory of = new no.altinn.services
                .serviceengine.correspondence._2009._10.ObjectFactory();
        GetCorrespondenceStatusDetailsV2 statusRequest = of.createGetCorrespondenceStatusDetailsV2();

        CorrespondenceStatusFilterV2 filter = new CorrespondenceStatusFilterV2();
        no.altinn.schemas.services.serviceengine.correspondence._2014._10.ObjectFactory filterOF = new no.altinn
                .schemas.services.serviceengine.correspondence._2014._10.ObjectFactory();
        JAXBElement<String> sendersReference = filterOF.createCorrespondenceStatusFilterV2SendersReference
                (conversation.getMessageId());
        filter.setSendersReference(sendersReference);
        filter.setServiceCode(conversation.getServiceCode());
        filter.setServiceEditionCode(Integer.parseInt(conversation.getServiceEditionCode()));
        statusRequest.setFilterCriteria(filter);

        return statusRequest;
    }

    private TextToken createTextToken(int num, String value) {
        TextToken textToken = new TextToken();
        textToken.setTokenNum(num);
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

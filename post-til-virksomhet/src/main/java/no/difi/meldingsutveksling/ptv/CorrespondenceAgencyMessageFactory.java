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
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister;
import no.difi.meldingsutveksling.nextmove.message.FileEntryStream;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import org.springframework.stereotype.Component;

import javax.activation.DataHandler;
import javax.xml.bind.JAXBElement;
import java.io.InputStream;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

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
    private final CryptoMessagePersister cryptoMessagePersister;
    private final Clock clock;

    @SneakyThrows
    public InsertCorrespondenceV2 create(NextMoveOutMessage message) {
        no.altinn.services.serviceengine.reporteeelementlist._2010._10.ObjectFactory reporteeFactory = new no.altinn.services.serviceengine.reporteeelementlist._2010._10.ObjectFactory();
        BinaryAttachmentExternalBEV2List attachmentExternalBEV2List = new BinaryAttachmentExternalBEV2List();

        for (BusinessMessageFile f : message.getFiles()) {
            FileEntryStream fileEntry = cryptoMessagePersister.readStream(message.getConversationId(), f.getIdentifier());
            BinaryAttachmentV2 binaryAttachmentV2 = new BinaryAttachmentV2();
            binaryAttachmentV2.setFunctionType(AttachmentFunctionType.fromValue("Unspecified"));
            binaryAttachmentV2.setFileName(reporteeFactory.createBinaryAttachmentV2FileName(f.getFilename()));
            binaryAttachmentV2.setName(reporteeFactory.createBinaryAttachmentV2Name(f.getTitle()));
            binaryAttachmentV2.setEncrypted(false);
            binaryAttachmentV2.setSendersReference(reporteeFactory.createBinaryAttachmentV2SendersReference("AttachmentReference_as123452"));
            binaryAttachmentV2.setData(reporteeFactory.createBinaryAttachmentV2Data(new DataHandler(InputStreamDataSource.of(fileEntry.getInputStream()))));
            attachmentExternalBEV2List.getBinaryAttachmentV2().add(binaryAttachmentV2);
        }

        if (message.getBusinessMessage() instanceof ArkivmeldingMessage) {
            BusinessMessageFile arkivmeldingFile = message.getFiles().stream()
                    .filter(f -> ARKIVMELDING_FILE.equals(f.getFilename()))
                    .findFirst()
                    .orElseThrow(() -> new NextMoveRuntimeException(String.format("%s not found for message %s", ARKIVMELDING_FILE, message.getConversationId())));
            InputStream is = cryptoMessagePersister.readStream(message.getConversationId(), arkivmeldingFile.getIdentifier()).getInputStream();
            Arkivmelding arkivmelding = ArkivmeldingUtil.unmarshalArkivmelding(is);

            Journalpost jp = ArkivmeldingUtil.getJournalpost(arkivmelding);
            return create(message.getConversationId(), message.getReceiverIdentifier(),
                    jp.getOffentligTittel(),
                    jp.getOffentligTittel(),
                    jp.getTittel(),
                    attachmentExternalBEV2List);
        }

        if (message.getBusinessMessage() instanceof DigitalDpvMessage) {
            DigitalDpvMessage msg = (DigitalDpvMessage) message.getBusinessMessage();
            return create(message.getConversationId(), message.getReceiverIdentifier(),
                    msg.getTitle(),
                    msg.getSummary(),
                    msg.getBody(),
                    attachmentExternalBEV2List);
        }

        throw new NextMoveRuntimeException(String.format("StandardBusinessDocument.any not instance of %s or %s, aborting",
                ArkivmeldingMessage.class.getName(), DigitalDpvMessage.class.getName()));
    }


    public InsertCorrespondenceV2 create(String conversationId,
                                         String receiverIdentifier,
                                         String messageTitle,
                                         String messageSummary,
                                         String messageBody,
                                         BinaryAttachmentExternalBEV2List attachments) {

        MyInsertCorrespondenceV2 correspondence = new MyInsertCorrespondenceV2();
        ObjectFactory objectFactory = new ObjectFactory();

        correspondence.setReportee(objectFactory.createMyInsertCorrespondenceV2Reportee(receiverIdentifier));
        // Service code, default 4255
        correspondence.setServiceCode(getServiceCode());
        // Service edition, default 10
        correspondence.setServiceEdition(getServiceEditionCode());
        // Should the user be allowed to forward the message from portal
        correspondence.setAllowForwarding(objectFactory.createMyInsertCorrespondenceV2AllowForwarding(false));
        // Name of the message sender, always "Avsender"
        correspondence.setMessageSender(objectFactory.createMyInsertCorrespondenceV2MessageSender(getSender()));
        // The date and time the message should be visible in the Portal
        correspondence.setVisibleDateTime(DateTimeUtil.toXMLGregorianCalendar(OffsetDateTime.now(clock)));
        correspondence.setDueDateTime(DateTimeUtil.toXMLGregorianCalendar(OffsetDateTime.now(clock).plusDays(7)));

        ExternalContentV2 externalContentV2 = new ExternalContentV2();
        externalContentV2.setLanguageCode(objectFactory.createExternalContentV2LanguageCode("1044"));
        externalContentV2.setMessageTitle(objectFactory.createExternalContentV2MessageTitle(messageTitle));
        externalContentV2.setMessageSummary(objectFactory.createExternalContentV2MessageSummary(messageSummary));
        externalContentV2.setMessageBody(objectFactory.createExternalContentV2MessageBody(messageBody));

        // The date and time the message can be deleted by the user
        correspondence.setAllowSystemDeleteDateTime(
                objectFactory.createMyInsertCorrespondenceV2AllowSystemDeleteDateTime(
                        DateTimeUtil.toXMLGregorianCalendar(getAllowSystemDeleteDateTime())));


        AttachmentsV2 attachmentsV2 = new AttachmentsV2();
        attachmentsV2.setBinaryAttachments(objectFactory.createAttachmentsV2BinaryAttachments(attachments));
        externalContentV2.setAttachments(objectFactory.createExternalContentV2Attachments(attachmentsV2));
        correspondence.setContent(objectFactory.createMyInsertCorrespondenceV2Content(externalContentV2));

        List<Notification2009> notificationList = createNotifications();

        NotificationBEList notifications = new NotificationBEList();
        List<Notification2009> notification = notifications.getNotification();
        notification.addAll(notificationList);
        correspondence.setNotifications(objectFactory.createMyInsertCorrespondenceV2Notifications(notifications));

        no.altinn.services.serviceengine.correspondence._2009._10.ObjectFactory correspondenceObjectFactory = new no.altinn.services.serviceengine.correspondence._2009._10.ObjectFactory();
        final InsertCorrespondenceV2 myInsertCorrespondenceV2 = correspondenceObjectFactory.createInsertCorrespondenceV2();
        myInsertCorrespondenceV2.setCorrespondence(correspondence);
        myInsertCorrespondenceV2.setSystemUserCode(config.getSystemUserCode());
        // Reference set by the message sender
        myInsertCorrespondenceV2.setExternalShipmentReference(conversationId);

        return myInsertCorrespondenceV2;
    }

    private List<Notification2009> createNotifications() {

        List<Notification2009> notifications = Lists.newArrayList();

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
        if (!isNullOrEmpty(config.getNotificationText())) {
            tokens.getTextToken().add(createTextToken(1, config.getNotificationText()));
        } else {
            tokens.getTextToken().add(createTextToken(1, String.format("Du har mottatt en melding fra %s.", getSender())));
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

    public GetCorrespondenceStatusDetailsV2 createReceiptRequest(String conversationId) {

        no.altinn.services.serviceengine.correspondence._2009._10.ObjectFactory of = new no.altinn.services
                .serviceengine.correspondence._2009._10.ObjectFactory();
        GetCorrespondenceStatusDetailsV2 statusRequest = of.createGetCorrespondenceStatusDetailsV2();

        CorrespondenceStatusFilterV2 filter = new CorrespondenceStatusFilterV2();
        no.altinn.schemas.services.serviceengine.correspondence._2014._10.ObjectFactory filterOF = new no.altinn
                .schemas.services.serviceengine.correspondence._2014._10.ObjectFactory();
        JAXBElement<String> sendersReference = filterOF.createCorrespondenceStatusFilterV2SendersReference
                (conversationId);
        filter.setSendersReference(sendersReference);
        filter.setServiceCode("4255");
        filter.setServiceEditionCode(10);
        statusRequest.setFilterCriteria(filter);

        return statusRequest;
    }

    private JAXBElement<String> getServiceCode() {
        String serviceCodeProp = config.getExternalServiceCode();
        String serviceCode = !isNullOrEmpty(serviceCodeProp) ? serviceCodeProp : "4255";
        ObjectFactory objectFactory = new ObjectFactory();
        return objectFactory.createMyInsertCorrespondenceV2ServiceCode(serviceCode);
    }

    private JAXBElement<String> getServiceEditionCode() {
        String serviceEditionProp = config.getExternalServiceEditionCode();
        String serviceEdition = !isNullOrEmpty(serviceEditionProp) ? serviceEditionProp : "10";
        ObjectFactory objectFactory = new ObjectFactory();
        return objectFactory.createMyInsertCorrespondenceV2ServiceEdition(serviceEdition);
    }

    private TextToken createTextToken(int num, String value) {
        no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory objectFactory = new no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory();
        TextToken textToken = new TextToken();
        textToken.setTokenNum(num);
        textToken.setTokenValue(objectFactory.createTextTokenTokenValue(value));

        return textToken;
    }

    private JAXBElement<ReceiverEndPointBEList> createReceiverEndPoint(TransportType type) {
        no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory objectFactory = new no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory();
        ReceiverEndPoint receiverEndPoint = new ReceiverEndPoint();
        receiverEndPoint.setTransportType(objectFactory.createReceiverEndPointTransportType(type));
        ReceiverEndPointBEList receiverEndpoints = new ReceiverEndPointBEList();
        receiverEndpoints.getReceiverEndPoint().add(receiverEndPoint);
        return objectFactory.createNotification2009ReceiverEndPoints(receiverEndpoints);
    }
}

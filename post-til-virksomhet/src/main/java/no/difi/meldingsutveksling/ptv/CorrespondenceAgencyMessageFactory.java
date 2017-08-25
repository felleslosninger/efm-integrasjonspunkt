package no.difi.meldingsutveksling.ptv;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
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
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreConverter;
import no.difi.meldingsutveksling.nextmove.DpvConversationResource;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import no.difi.meldingsutveksling.receipt.Conversation;
import org.apache.commons.io.FileUtils;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Class used to create an InsertCorrespondenceV2 object based on an internal message format.
 */
public class CorrespondenceAgencyMessageFactory {

    private static final Map<Integer, String> serviceEditionMapping= new HashMap<>();

    static {
        serviceEditionMapping.put(1, "Plan, bygg og geodata");
        serviceEditionMapping.put(2, "Helse, sosial og omsorg");
        serviceEditionMapping.put(3, "Oppvekst og utdanning");
        serviceEditionMapping.put(4, "Kultur, idrett og fritid");
        serviceEditionMapping.put(5, "Trafikk, reiser og samferdsel");
        serviceEditionMapping.put(6, "Natur og miljø");
        serviceEditionMapping.put(7, "Næringsutvikling");
        serviceEditionMapping.put(8, "Skatter og avgifter");
        serviceEditionMapping.put(9, "Tekniske tjenester");
        serviceEditionMapping.put(10, "Administrasjon");
    }

    private CorrespondenceAgencyMessageFactory() {
    }

    public static InsertCorrespondenceV2 create(CorrespondenceAgencyConfiguration config, DpvConversationResource cr) throws NextMoveException {

        no.altinn.services.serviceengine.reporteeelementlist._2010._10.ObjectFactory reporteeFactory = new no.altinn.services.serviceengine.reporteeelementlist._2010._10.ObjectFactory();
        BinaryAttachmentExternalBEV2List attachmentExternalBEV2List = new BinaryAttachmentExternalBEV2List();

        for (String f : cr.getFileRefs().values()) {
            String filedir = config.getNextbestFiledir();
            if (!filedir.endsWith("/")) {
                filedir = filedir + "/";
            }
            filedir = filedir + cr.getConversationId() + "/";
            File file = new File(filedir + f);

            byte[] bytes;
            try {
                bytes = FileUtils.readFileToByteArray(file);
            } catch (IOException e) {
                throw new NextMoveException(String.format("Could not read file \"%s\"", f), e);
            }

            BinaryAttachmentV2 binaryAttachmentV2 = new BinaryAttachmentV2();
            binaryAttachmentV2.setFunctionType(AttachmentFunctionType.fromValue("Unspecified"));
            binaryAttachmentV2.setFileName(reporteeFactory.createBinaryAttachmentV2FileName(f));
            binaryAttachmentV2.setName(reporteeFactory.createBinaryAttachmentV2Name(f));
            binaryAttachmentV2.setEncrypted(false);
            binaryAttachmentV2.setSendersReference(reporteeFactory.createBinaryAttachmentV2SendersReference("AttachmentReference_as123452"));
            binaryAttachmentV2.setData(reporteeFactory.createBinaryAttachmentV2Data(bytes));
            attachmentExternalBEV2List.getBinaryAttachmentV2().add(binaryAttachmentV2);
        }

        return create(config, cr.getConversationId(), cr.getReceiverId(), cr.getReceiverName(),
                cr.getMessageTitle(), cr.getMessageContent(), attachmentExternalBEV2List);
    }

    public static InsertCorrespondenceV2 create(CorrespondenceAgencyConfiguration config, EDUCore edu) {

        no.altinn.services.serviceengine.reporteeelementlist._2010._10.ObjectFactory reporteeFactory = new no.altinn.services.serviceengine.reporteeelementlist._2010._10.ObjectFactory();
        BinaryAttachmentExternalBEV2List attachmentExternalBEV2List = new BinaryAttachmentExternalBEV2List();
        MeldingType meldingType = new EDUCoreConverter().payloadAsMeldingType(edu.getPayload());
        meldingType.getJournpost().getDokument().forEach(d -> {
            BinaryAttachmentV2 binaryAttachmentV2 = new BinaryAttachmentV2();
            binaryAttachmentV2.setFunctionType(AttachmentFunctionType.fromValue("Unspecified"));
            binaryAttachmentV2.setFileName(reporteeFactory.createBinaryAttachmentV2FileName(d.getVeFilnavn()));
            binaryAttachmentV2.setName(reporteeFactory.createBinaryAttachmentV2Name(d.getVeFilnavn()));
            binaryAttachmentV2.setEncrypted(false);
            binaryAttachmentV2.setSendersReference(reporteeFactory.createBinaryAttachmentV2SendersReference("AttachmentReference_as123452"));
            binaryAttachmentV2.setData(reporteeFactory.createBinaryAttachmentV2Data(d.getFil().getBase64()));
            attachmentExternalBEV2List.getBinaryAttachmentV2().add(binaryAttachmentV2);
        });
        String title = meldingType.getJournpost().getJpInnhold();
        String content = meldingType.getJournpost().getJpOffinnhold();

        return create(config, edu.getId(), edu.getReceiver().getIdentifier(), edu.getReceiver().getName(), title,
                content, attachmentExternalBEV2List);
    }

    public static InsertCorrespondenceV2 create(CorrespondenceAgencyConfiguration config,
                                                String conversationId,
                                                String receiverIdentifier,
                                                String receiverName,
                                                String messageTitle,
                                                String messageContent,
                                                BinaryAttachmentExternalBEV2List attachments) {

        MyInsertCorrespondenceV2 correspondence = new MyInsertCorrespondenceV2();
        ObjectFactory objectFactory = new ObjectFactory();

        correspondence.setReportee(objectFactory.createMyInsertCorrespondenceV2Reportee(receiverIdentifier));
        // Service code, default 4255
        correspondence.setServiceCode(getServiceCode(config));
        // Service edition, default 10
        correspondence.setServiceEdition(getServiceEditionCode(config));
        // Should the user be allowed to forward the message from portal
        correspondence.setAllowForwarding(objectFactory.createMyInsertCorrespondenceV2AllowForwarding(false));
        // Name of the message sender, always "Avsender"
        correspondence.setMessageSender(objectFactory.createMyInsertCorrespondenceV2MessageSender(config.getSender()));
        // The date and time the message should be visible in the Portal
        correspondence.setVisibleDateTime(toXmlGregorianCalendar(ZonedDateTime.now()));
        correspondence.setDueDateTime(toXmlGregorianCalendar(ZonedDateTime.now().plusDays(7)));

        ExternalContentV2 externalContentV2 = new ExternalContentV2();
        externalContentV2.setLanguageCode(objectFactory.createExternalContentV2LanguageCode("1044"));
        externalContentV2.setMessageTitle(objectFactory.createExternalContentV2MessageTitle(messageTitle));
        externalContentV2.setMessageSummary(objectFactory.createExternalContentV2MessageSummary(messageTitle));
        externalContentV2.setMessageBody(objectFactory.createExternalContentV2MessageBody(messageContent));

        // The date and time the message can be deleted by the user
        correspondence.setAllowSystemDeleteDateTime(
                objectFactory.createMyInsertCorrespondenceV2AllowSystemDeleteDateTime(
                        toXmlGregorianCalendar(getAllowSystemDeleteDateTime())));



        AttachmentsV2 attachmentsV2 = new AttachmentsV2();
        attachmentsV2.setBinaryAttachments(objectFactory.createAttachmentsV2BinaryAttachments(attachments));
        externalContentV2.setAttachments(objectFactory.createExternalContentV2Attachments(attachmentsV2));
        correspondence.setContent(objectFactory.createMyInsertCorrespondenceV2Content(externalContentV2));

        List<Notification2009> notificationList = createNotifications(config, receiverName);

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

    private static List<Notification2009> createNotifications(CorrespondenceAgencyConfiguration config, String receiverName) {

        List<Notification2009> notifications = Lists.newArrayList();

        if (config.isNotifyEmail() && config.isNotifySms()) {
            notifications.add(createNotification(config, receiverName, TransportType.EMAIL, 0));
            notifications.add(createNotification(config, receiverName, TransportType.EMAIL, 7));
            notifications.add(createNotification(config, receiverName, TransportType.SMS, 0));
            notifications.add(createNotification(config, receiverName, TransportType.SMS, 7));
        } else if (config.isNotifySms()) {
            notifications.add(createNotification(config, receiverName, TransportType.SMS, 0));
            notifications.add(createNotification(config, receiverName, TransportType.SMS, 7));
        } else if (config.isNotifyEmail()){
            notifications.add(createNotification(config, receiverName, TransportType.EMAIL, 0));
            notifications.add(createNotification(config, receiverName, TransportType.EMAIL, 7));
        } else {
            notifications.add(createNotification(config, receiverName, null, 0));
            notifications.add(createNotification(config, receiverName, null, 7));
        }

        return notifications;
    }

    private static Notification2009 createNotification(CorrespondenceAgencyConfiguration config, String receiverName,
                                                       TransportType type, int delayInDays) {

        Notification2009 notification = new Notification2009();
        no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory notificationFactory = new no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory();
        notification.setFromAddress(notificationFactory.createNotification2009FromAddress("no-reply@altinn.no"));
        // The date and time the notification should be sent
        if (delayInDays == 0) {
            notification.setShipmentDateTime(toXmlGregorianCalendar(ZonedDateTime.now().plusMinutes(5)));
        } else {
            notification.setShipmentDateTime(toXmlGregorianCalendar(ZonedDateTime.now().plusDays(delayInDays)));
        }
        // Language code of the notification
        notification.setLanguageCode(notificationFactory.createNotification2009LanguageCode("1044"));
        // Notification type
        if (type == null) {
            notification.setNotificationType(notificationFactory.createNotification2009NotificationType("offentlig_etat"));
            notification.setTextTokens(notificationFactory.createNotification2009TextTokens(createTokens(config, null,
                    receiverName)));
            JAXBElement<ReceiverEndPointBEList> receiverEndpoints = createReceiverEndPoint(TransportType.EMAIL);
            notification.setReceiverEndPoints(receiverEndpoints);
        } else {
            notification.setNotificationType(notificationFactory.createNotification2009NotificationType("TokenTextOnly"));
            notification.setTextTokens(notificationFactory.createNotification2009TextTokens(createTokens(config, type,
                    receiverName)));
            JAXBElement<ReceiverEndPointBEList> receiverEndpoints = createReceiverEndPoint(type);
            notification.setReceiverEndPoints(receiverEndpoints);
        }

        return notification;
    }

    private static TextTokenSubstitutionBEList createTokens(CorrespondenceAgencyConfiguration config,
                                                                TransportType type,
                                                                String receiverName) {

        TextTokenSubstitutionBEList tokens = new TextTokenSubstitutionBEList();

        if (type == null) {
            tokens.getTextToken().add(createTextToken(0, config.getSender()));
            tokens.getTextToken().add(createTextToken(1, serviceEditionMapping.get(Integer.valueOf(getServiceEditionCode
                    (config).getValue()))));
            tokens.getTextToken().add(createTextToken(2, receiverName));
            return tokens;
        }

        switch (type) {
            case EMAIL:
                tokens.getTextToken().add(createTextToken(0, config.getEmailSubject()));
                tokens.getTextToken().add(createTextToken(1, config.getEmailBody()));
                break;
            case SMS:
                tokens.getTextToken().add(createTextToken(0, config.getSmsText()));
                tokens.getTextToken().add(createTextToken(1, ""));
                break;
            default:
                tokens.getTextToken().add(createTextToken(0, config.getSender()));
                tokens.getTextToken().add(createTextToken(1, serviceEditionMapping.get(Integer.valueOf(getServiceEditionCode
                        (config).getValue()))));
                tokens.getTextToken().add(createTextToken(2, receiverName));
        }

        return tokens;
    }

    private static ZonedDateTime getAllowSystemDeleteDateTime() {
        return ZonedDateTime.now().plusMinutes(5);
    }

    public static GetCorrespondenceStatusDetailsV2 createReceiptRequest(Conversation conversation) {

        no.altinn.services.serviceengine.correspondence._2009._10.ObjectFactory of = new no.altinn.services
                .serviceengine.correspondence._2009._10.ObjectFactory();
        GetCorrespondenceStatusDetailsV2 statusRequest = of.createGetCorrespondenceStatusDetailsV2();

        CorrespondenceStatusFilterV2 filter = new CorrespondenceStatusFilterV2();
        no.altinn.schemas.services.serviceengine.correspondence._2014._10.ObjectFactory filterOF = new no.altinn
                .schemas.services.serviceengine.correspondence._2014._10.ObjectFactory();
        JAXBElement<String> sendersReference = filterOF.createCorrespondenceStatusFilterV2SendersReference
                (conversation.getConversationId());
        filter.setSendersReference(sendersReference);
        filter.setServiceCode("4255");
        filter.setServiceEditionCode(10);
        statusRequest.setFilterCriteria(filter);

        return statusRequest;
    }

    private static JAXBElement<String> getServiceCode(CorrespondenceAgencyConfiguration postConfig) {
        String serviceCodeProp= postConfig.getExternalServiceCode();
        String serviceCode= !Strings.isNullOrEmpty(serviceCodeProp) ? serviceCodeProp : "4255";
        ObjectFactory objectFactory = new ObjectFactory();
        return objectFactory.createMyInsertCorrespondenceV2ServiceCode(serviceCode);
    }

    private static JAXBElement<String> getServiceEditionCode(CorrespondenceAgencyConfiguration postConfig) {
        String serviceEditionProp = postConfig.getExternalServiceEditionCode();
        String serviceEdition = !Strings.isNullOrEmpty(serviceEditionProp) ? serviceEditionProp : "10";
        ObjectFactory objectFactory = new ObjectFactory();
        return objectFactory.createMyInsertCorrespondenceV2ServiceEdition(serviceEdition);
    }

    private static TextToken createTextToken(int num, String value) {
        no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory objectFactory = new no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory();
        TextToken textToken = new TextToken();
        textToken.setTokenNum(num);
        textToken.setTokenValue(objectFactory.createTextTokenTokenValue(value));

        return textToken;
    }

    private static JAXBElement<ReceiverEndPointBEList> createReceiverEndPoint(TransportType type) {
        no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory objectFactory = new no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory();
        ReceiverEndPoint receiverEndPoint = new ReceiverEndPoint();
        receiverEndPoint.setTransportType(objectFactory.createReceiverEndPointTransportType(type));
        ReceiverEndPointBEList receiverEndpoints = new ReceiverEndPointBEList();
        receiverEndpoints.getReceiverEndPoint().add(receiverEndPoint);
        return objectFactory.createNotification2009ReceiverEndPoints(receiverEndpoints);
    }

    private static XMLGregorianCalendar toXmlGregorianCalendar(ZonedDateTime date) {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(date));
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException("Could not convert ZonedDateTime(value="+date+") to " + XMLGregorianCalendar.class, e);
        }
    }

}

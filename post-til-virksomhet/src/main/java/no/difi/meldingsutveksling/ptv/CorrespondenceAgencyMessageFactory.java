package no.difi.meldingsutveksling.ptv;

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
import no.difi.meldingsutveksling.nextmove.FileAttachement;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import no.difi.meldingsutveksling.receipt.Conversation;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static com.google.common.base.Strings.isNullOrEmpty;

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

    public static InsertCorrespondenceV2 create(CorrespondenceAgencyConfiguration config,
                                                DpvConversationResource cr,
                                                MessagePersister persister) throws NextMoveException {

        no.altinn.services.serviceengine.reporteeelementlist._2010._10.ObjectFactory reporteeFactory = new no.altinn.services.serviceengine.reporteeelementlist._2010._10.ObjectFactory();
        BinaryAttachmentExternalBEV2List attachmentExternalBEV2List = new BinaryAttachmentExternalBEV2List();

        for (String f : cr.getFileRefs().values()) {
            byte[] bytes;
            try {
                bytes = persister.read(cr, f);
            } catch (IOException e) {
                throw new NextMoveException(String.format("Could not read file \"%s\"", f), e);
            }

            Optional<FileAttachement> fao = Optional.empty();
            if (cr.getFiles() != null) {
                fao = cr.getFiles().stream().filter(fa -> f.equals(fa.getFilnavn())).findAny();
            }
            BinaryAttachmentV2 binaryAttachmentV2 = new BinaryAttachmentV2();
            binaryAttachmentV2.setFunctionType(AttachmentFunctionType.fromValue("Unspecified"));
            binaryAttachmentV2.setFileName(reporteeFactory.createBinaryAttachmentV2FileName(f));
            binaryAttachmentV2.setName(reporteeFactory.createBinaryAttachmentV2Name(fao.map(FileAttachement::getTittel).orElse(f)));
            binaryAttachmentV2.setEncrypted(false);
            binaryAttachmentV2.setSendersReference(reporteeFactory.createBinaryAttachmentV2SendersReference("AttachmentReference_as123452"));
            binaryAttachmentV2.setData(reporteeFactory.createBinaryAttachmentV2Data(bytes));
            attachmentExternalBEV2List.getBinaryAttachmentV2().add(binaryAttachmentV2);
        }

        return create(config, cr.getConversationId(), cr.getReceiver().getReceiverId(), cr.getMessageTitle(),
                cr.getMessageSummary(), cr.getMessageBody(), attachmentExternalBEV2List);
    }

    public static InsertCorrespondenceV2 create(CorrespondenceAgencyConfiguration config, EDUCore edu) {

        no.altinn.services.serviceengine.reporteeelementlist._2010._10.ObjectFactory reporteeFactory = new no.altinn.services.serviceengine.reporteeelementlist._2010._10.ObjectFactory();
        BinaryAttachmentExternalBEV2List attachmentExternalBEV2List = new BinaryAttachmentExternalBEV2List();
        MeldingType meldingType = EDUCoreConverter.payloadAsMeldingType(edu.getPayload());
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

        return create(config, edu.getId(), edu.getReceiver().getIdentifier(), title, title, content, attachmentExternalBEV2List);
    }

    public static InsertCorrespondenceV2 create(CorrespondenceAgencyConfiguration config,
                                                String conversationId,
                                                String receiverIdentifier,
                                                String messageTitle,
                                                String messageSummary,
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
        correspondence.setAllowForwarding(objectFactory.createMyInsertCorrespondenceV2AllowForwarding(config.isAllowForwarding()));
        // Name of the message sender, always "Avsender"
        correspondence.setMessageSender(objectFactory.createMyInsertCorrespondenceV2MessageSender(config.getSender()));
        // The date and time the message should be visible in the Portal
        correspondence.setVisibleDateTime(getVisibleDateTime(config));
        correspondence.setDueDateTime(toXmlGregorianCalendar(ZonedDateTime.now().plusDays(7)));

        ExternalContentV2 externalContentV2 = new ExternalContentV2();
        externalContentV2.setLanguageCode(objectFactory.createExternalContentV2LanguageCode(getLanguageCode(config)));
        externalContentV2.setMessageTitle(objectFactory.createExternalContentV2MessageTitle(messageTitle));
        externalContentV2.setMessageSummary(objectFactory.createExternalContentV2MessageSummary(messageSummary));
        externalContentV2.setMessageBody(objectFactory.createExternalContentV2MessageBody(messageContent));

        // The date and time the message can be deleted by the user
        correspondence.setAllowSystemDeleteDateTime(
                objectFactory.createMyInsertCorrespondenceV2AllowSystemDeleteDateTime(
                        getAllowSystemDeleteDateTime(config)));

        AttachmentsV2 attachmentsV2 = new AttachmentsV2();
        attachmentsV2.setBinaryAttachments(objectFactory.createAttachmentsV2BinaryAttachments(attachments));
        externalContentV2.setAttachments(objectFactory.createExternalContentV2Attachments(attachmentsV2));
        correspondence.setContent(objectFactory.createMyInsertCorrespondenceV2Content(externalContentV2));

        List<Notification2009> notificationList = createNotifications(config);

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

    private static XMLGregorianCalendar getVisibleDateTime(CorrespondenceAgencyConfiguration config) {
        if (config.getVisibleDateTime() != null) {
            return toXmlGregorianCalendar(config.getVisibleDateTime().atZone(ZoneId.systemDefault()));
        }
        return toXmlGregorianCalendar(ZonedDateTime.now());
    }

    private static XMLGregorianCalendar getAllowSystemDeleteDateTime(CorrespondenceAgencyConfiguration config) {
        if (config.getAllowSystemDeleteTime() != null) {
            return toXmlGregorianCalendar(config.getAllowSystemDeleteTime().atZone(ZoneId.systemDefault()));
        }
        return toXmlGregorianCalendar(ZonedDateTime.now().plusMinutes(5));
    }


    private static String getLanguageCode(CorrespondenceAgencyConfiguration config) {
        if (!isNullOrEmpty(config.getLanguageCode())) {
            return config.getLanguageCode();
        }
        return "1044";
    }

    private static List<Notification2009> createNotifications(CorrespondenceAgencyConfiguration config) {

        List<Notification2009> notifications = Lists.newArrayList();

        if (!isNullOrEmpty(config.getEmailText()) || config.isNotifyEmail()) {
            notifications.add(createNotification(config, TransportType.EMAIL));
        }
        if (!isNullOrEmpty(config.getSmsText()) || config.isNotifySms()) {
            notifications.add(createNotification(config, TransportType.SMS));
        }

        return notifications;
    }

    private static Notification2009 createNotification(CorrespondenceAgencyConfiguration config, TransportType type) {

        Notification2009 notification = new Notification2009();
        no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory notificationFactory = new no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory();
        notification.setFromAddress(notificationFactory.createNotification2009FromAddress("no-reply@altinn.no"));
        // The date and time the notification should be sent
        notification.setShipmentDateTime(toXmlGregorianCalendar(ZonedDateTime.now().plusMinutes(5)));
        // Language code of the notification
        notification.setLanguageCode(notificationFactory.createNotification2009LanguageCode(getLanguageCode(config)));
        // Notification type
        notification.setNotificationType(notificationFactory.createNotification2009NotificationType("VarselDPVMedRevarsel"));
        notification.setTextTokens(notificationFactory.createNotification2009TextTokens(createTokens(config, type)));
        JAXBElement<ReceiverEndPointBEList> receiverEndpoints = createReceiverEndPoint(type);
        notification.setReceiverEndPoints(receiverEndpoints);

        return notification;
    }

    private static TextTokenSubstitutionBEList createTokens(CorrespondenceAgencyConfiguration config, TransportType type) {

        TextTokenSubstitutionBEList tokens = new TextTokenSubstitutionBEList();

        if (type.equals(TransportType.EMAIL) && !isNullOrEmpty(config.getEmailText())) {
            tokens.getTextToken().add(createTextToken(1, config.getEmailText()));
        } else if (type.equals(TransportType.SMS) && !isNullOrEmpty(config.getSmsText())) {
            tokens.getTextToken().add(createTextToken(1, config.getSmsText()));
        } else if (!isNullOrEmpty(config.getNotificationText())) {
            tokens.getTextToken().add(createTextToken(1, config.getNotificationText()));
        } else {
            tokens.getTextToken().add(createTextToken(1, String.format("Du har mottatt en melding fra %s.", config.getSender())));
        }

        return tokens;
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

    private static JAXBElement<String> getServiceCode(CorrespondenceAgencyConfiguration config) {
        String serviceCodeProp= config.getExternalServiceCode();
        String serviceCode= !isNullOrEmpty(serviceCodeProp) ? serviceCodeProp : "4255";
        ObjectFactory objectFactory = new ObjectFactory();
        return objectFactory.createMyInsertCorrespondenceV2ServiceCode(serviceCode);
    }

    private static JAXBElement<String> getServiceEditionCode(CorrespondenceAgencyConfiguration config) {
        String serviceEditionProp = config.getExternalServiceEditionCode();
        String serviceEdition = !isNullOrEmpty(serviceEditionProp) ? serviceEditionProp : "10";
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

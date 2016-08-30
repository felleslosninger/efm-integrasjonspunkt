package no.difi.meldingsutveksling.ptv.mapping;

import com.google.common.base.MoreObjects;
import no.difi.meldingsutveksling.mxa.schema.domain.Message;
import no.difi.meldingsutveksling.noarkexchange.PayloadException;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static no.difi.meldingsutveksling.noarkexchange.PayloadUtil.queryPayload;

/**
 * Internal representation of the message values, used for mapping.
 */
public class CorrespondenceAgencyValues {

    private String senderOrgNr;
    private String senderOrgName;
    private String receiverOrgNr;
    private String receiverOrgName;
    private String reportee;
    private String messageTitle;
    private String messageSummary;
    private String messageBody;
    private String externalShipmentReference;
    private ZonedDateTime allowSystemDeleteDateTime;

    private List<Attachment> attachments;

    private CorrespondenceAgencyValues() {
    }

    public static CorrespondenceAgencyValues from(PutMessageRequestWrapper putMessage, InfoRecord senderInfo, InfoRecord receiverInfo) throws PayloadException {
        String xpathJpInnhold= "Melding/journpost/jpInnhold";
        String xpathJpOffinnhold= "Melding/journpost/jpOffinnhold";
        String xpathJpFilnavn= "Melding/journpost/dokument/veFilnavn";
        String xpathJpData= "Melding/journpost/dokument/fil/base64";

        CorrespondenceAgencyValues values = new CorrespondenceAgencyValues();
        values.setSenderOrgNr(senderInfo.getOrganisationNumber());
        values.setSenderOrgName(senderInfo.getOrganizationName());
        values.setReceiverOrgNr(receiverInfo.getOrganisationNumber());
        values.setReceiverOrgName(receiverInfo.getOrganizationName());

        values.setReportee(putMessage.getEnvelope().getReceiver().getOrgnr());
        values.setMessageTitle(queryPayload(putMessage, xpathJpInnhold));
        values.setMessageSummary(queryPayload(putMessage, xpathJpInnhold));
        values.setMessageBody(queryPayload(putMessage, xpathJpOffinnhold));
        values.setAllowSystemDeleteDateTime(ZonedDateTime.now().plusMinutes(5));
        values.setExternalShipmentReference(putMessage.getEnvelope().getConversationId());

        Attachment attachment = new Attachment(queryPayload(putMessage, xpathJpFilnavn),
                "Brev",
                Base64.getDecoder().decode(queryPayload(putMessage, xpathJpData)));
        values.setAttachments(Arrays.asList(attachment));

        return values;
    }

    public static CorrespondenceAgencyValues from(Message msg, InfoRecord senderInfo, InfoRecord receiverInfo) {
        CorrespondenceAgencyValues values = new CorrespondenceAgencyValues();
        values.setSenderOrgNr(senderInfo.getOrganisationNumber());
        values.setSenderOrgName(senderInfo.getOrganizationName());
        values.setReceiverOrgNr(receiverInfo.getOrganisationNumber());
        values.setReceiverOrgName(receiverInfo.getOrganizationName());

        values.setReportee(msg.getParticipantId());
        values.setMessageTitle(msg.getContent().getMessageHeader());
        values.setMessageSummary(msg.getContent().getMessageHeader());
        values.setMessageBody(msg.getContent().getMessageSummery());
        values.setAllowSystemDeleteDateTime(ZonedDateTime.now().plusYears(5));
        values.setExternalShipmentReference(msg.getMessageReference());

        final ArrayList<Attachment> attachments = new ArrayList<>();
        if (msg.getContent().getAttachments() != null && msg.getContent().getAttachments().getAttachment().size() > 0) {
            msg.getContent().getAttachments().getAttachment().forEach(a -> {
                attachments.add(new Attachment(a.getFilename(), a.getName(), Base64.getDecoder().decode(a.getValue())));
            });
            values.setAttachments(attachments);
        }

        return values;
    }

    public String getSenderOrgNr() {
        return senderOrgNr;
    }

    public void setSenderOrgNr(String senderOrgNr) {
        this.senderOrgNr = senderOrgNr;
    }

    public String getSenderOrgName() {
        return senderOrgName;
    }

    public void setSenderOrgName(String senderOrgName) {
        this.senderOrgName = senderOrgName;
    }

    public String getReceiverOrgNr() {
        return receiverOrgNr;
    }

    public void setReceiverOrgNr(String receiverOrgNr) {
        this.receiverOrgNr = receiverOrgNr;
    }

    public String getReceiverOrgName() {
        return receiverOrgName;
    }

    public void setReceiverOrgName(String receiverOrgName) {
        this.receiverOrgName = receiverOrgName;
    }

    public String getMessageTitle() {
        return messageTitle;
    }

    public void setMessageTitle(String messageTitle) {
        this.messageTitle = messageTitle;
    }

    public String getMessageSummary() {
        return messageSummary;
    }

    public void setMessageSummary(String messageSummary) {
        this.messageSummary = messageSummary;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public ZonedDateTime getAllowSystemDeleteDateTime() {
        return allowSystemDeleteDateTime;
    }

    public void setAllowSystemDeleteDateTime(ZonedDateTime allowSystemDeleteDateTime) {
        this.allowSystemDeleteDateTime = allowSystemDeleteDateTime;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }
    
    public String getExternalShipmentReference() {
        return externalShipmentReference;
    }

    public void setExternalShipmentReference(String externalShipmentReference) {
        this.externalShipmentReference = externalShipmentReference;
    }

    public String getReportee() {
        return reportee;
    }

    public void setReportee(String reportee) {
        this.reportee = reportee;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("senderOrgNr", senderOrgNr)
                .add("senderOrgName", senderOrgName)
                .add("receiverOrgNr", receiverOrgNr)
                .add("receiverOrgName", receiverOrgName)
                .add("reportee", reportee)
                .add("messageTitle", messageTitle)
                .add("messageSummary", messageSummary)
                .add("messageBody", messageBody)
                .add("allowSystemDeleteDateTime", allowSystemDeleteDateTime)
                .add("externalShipmentReference", externalShipmentReference)
                .add("attachments", attachments)
                .toString();
    }
}

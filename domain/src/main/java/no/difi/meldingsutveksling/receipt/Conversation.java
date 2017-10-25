package no.difi.meldingsutveksling.receipt;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import no.difi.meldingsutveksling.noarkexchange.PayloadException;
import no.difi.meldingsutveksling.noarkexchange.PayloadUtil;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPE_INNSYN;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;
import static no.difi.meldingsutveksling.receipt.ConversationMarker.markerFrom;

@Entity
@Data
@Slf4j
public class Conversation {

    @Id
    @GeneratedValue
    private Integer convId;
    private String conversationId;
    private String receiverIdentifier;
    private String messageReference;
    private String messageTitle;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime lastUpdate;
    @JsonIgnore
    private boolean pollable;
    private boolean finished;
    private ServiceIdentifier serviceIdentifier;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "conv_id")
    private List<MessageStatus> messageStatuses;

    Conversation() {}

    private Conversation(String conversationId,
                         String messageReference,
                         String receiverIdentifier,
                         String messageTitle,
                         ServiceIdentifier serviceIdentifier) {
        this.conversationId = conversationId;
        this.messageReference = messageReference;
        this.receiverIdentifier = receiverIdentifier;
        this.messageTitle = messageTitle;
        this.messageStatuses = Lists.newArrayList();
        this.serviceIdentifier = serviceIdentifier;
        this.lastUpdate = LocalDateTime.now();
    }

    public static Conversation of(String conversationId,
                                  String messageReference,
                                  String receiverIdentifier,
                                  String messageTitle,
                                  ServiceIdentifier serviceIdentifier,
                                  MessageStatus... statuses) {

        Conversation c = new Conversation(conversationId, messageReference, receiverIdentifier, messageTitle, serviceIdentifier);
        if (statuses != null && statuses.length > 0) {
            Stream.of(statuses)
                    .peek(r -> r.setConversationId(conversationId))
                    .forEach(c::addMessageStatus);
        }
        return c;
    }

    public static Conversation of(ConversationResource cr, MessageStatus... statuses) {
        Conversation c = new Conversation(cr.getConversationId(), cr.getConversationId(), cr.getReceiverId(),
                "", cr.getServiceIdentifier());
        if (statuses != null && statuses.length > 0) {
            Stream.of(statuses)
                    .peek(r -> r.setConversationId(cr.getConversationId()))
                    .forEach(c::addMessageStatus);
        }
        return c;

    }

    public static Conversation of(EDUCore eduCore, MessageStatus... statuses) {
        String msgTitle = "";
        if (eduCore.getMessageType() == EDUCore.MessageType.EDU) {
            String jpInnholdXpath = "Melding/journpost/jpInnhold";
            try {
                msgTitle = PayloadUtil.queryPayload(eduCore.getPayload(), jpInnholdXpath);
            } catch (PayloadException e) {
                log.error("Could not read jpInnhold from payload", e);
            }
        }

        Conversation c = new Conversation(eduCore.getId(), eduCore.getMessageReference(),
                eduCore.getReceiver().getIdentifier(), msgTitle,
                eduCore.getServiceIdentifier() == DPE_INNSYN ? DPV : eduCore.getServiceIdentifier());

        if (statuses != null && statuses.length > 0) {
            Stream.of(statuses)
                    .peek(r -> r.setConversationId(eduCore.getId()))
                    .forEach(c::addMessageStatus);
        }
        return c;
    }

    public void addMessageStatus(MessageStatus status) {
        status.setConversationId(getConversationId());
        this.messageStatuses.add(status);
        this.lastUpdate = LocalDateTime.now();
        log.debug(String.format("Conversation [%s] updated with status %s", getConversationId(), status.getStatus()), markerFrom(this));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("convId", convId)
                .add("conversationId", conversationId)
                .add("messageReference", messageReference)
                .add("receiverIdentifier", receiverIdentifier)
                .add("messageReference", messageReference)
                .add("messageTitle", messageTitle)
                .add("pollable", pollable)
                .add("serviceIdentifier", serviceIdentifier)
                .add("messageStatuses", messageStatuses)
                .toString();
    }
}

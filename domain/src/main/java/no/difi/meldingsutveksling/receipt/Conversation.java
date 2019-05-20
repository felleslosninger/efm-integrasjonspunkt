package no.difi.meldingsutveksling.receipt;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.MessageInformable;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import no.difi.meldingsutveksling.noarkexchange.PayloadException;
import no.difi.meldingsutveksling.noarkexchange.PayloadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPE;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;
import static no.difi.meldingsutveksling.receipt.ConversationMarker.markerFrom;

@Entity
@Data
@Slf4j
@Table(name = "conversation",
        indexes = {@Index(columnList = "conversation_id")})
public class Conversation implements MessageInformable {

    public static final Logger statusLogger = LoggerFactory.getLogger("STATUS");

    @Id
    @GeneratedValue
    private Integer convId;
    @Column(name = "conversation_id")
    private String conversationId;
    private String senderIdentifier;
    private String receiverIdentifier;
    private String messageReference;
    private String messageTitle;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime lastUpdate;
    @JsonIgnore
    private boolean pollable;
    private boolean finished;
    private boolean msh;
    private ConversationDirection direction;
    private ServiceIdentifier serviceIdentifier;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "conv_id")
    private List<MessageStatus> messageStatuses;

    Conversation() {
    }

    private Conversation(String conversationId,
                         String messageReference,
                         String senderIdentifier,
                         String receiverIdentifier,
                         ConversationDirection direction,
                         String messageTitle,
                         ServiceIdentifier serviceIdentifier) {
        this.conversationId = conversationId;
        this.messageReference = messageReference;
        this.senderIdentifier = senderIdentifier;
        this.receiverIdentifier = receiverIdentifier;
        this.direction = direction;
        this.messageTitle = messageTitle;
        this.messageStatuses = Lists.newArrayList();
        this.serviceIdentifier = serviceIdentifier;
        this.lastUpdate = LocalDateTime.now();
    }

    private Conversation addMessageStatuses(MessageStatus... statuses) {
        if (statuses != null && statuses.length > 0) {
            Stream.of(statuses)
                    .peek(r -> r.setConversationId(conversationId))
                    .forEach(this::addMessageStatus);
        }
        return this;
    }

    public static Conversation of(String conversationId,
                                  String messageReference,
                                  String senderIdentifier,
                                  String receiverIdentifier,
                                  ConversationDirection direction,
                                  String messageTitle,
                                  ServiceIdentifier serviceIdentifier,
                                  MessageStatus... statuses) {

        return new Conversation(conversationId, messageReference, senderIdentifier, receiverIdentifier, direction, messageTitle, serviceIdentifier)
                .addMessageStatuses(statuses);
    }


    public static Conversation of(MessageInformable msg, MessageStatus... statuses) {
        return new Conversation(msg.getConversationId(), msg.getConversationId(), msg.getSenderIdentifier(), msg.getReceiverIdentifier(),
                msg.getDirection(), "", msg.getServiceIdentifier())
                .addMessageStatuses(statuses);
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

        return new Conversation(eduCore.getId(), eduCore.getMessageReference(),
                eduCore.getSender().getIdentifier(), eduCore.getReceiver().getIdentifier(), ConversationDirection.OUTGOING,
                msgTitle, eduCore.getServiceIdentifier() == DPE ? DPV : eduCore.getServiceIdentifier())
                .addMessageStatuses(statuses);
    }

    Conversation addMessageStatus(MessageStatus status) {
        status.setConversationId(getConversationId());
        this.messageStatuses.add(status);
        this.lastUpdate = LocalDateTime.now();
        statusLogger.info(markerFrom(this).and(markerFrom(status)), String.format("Conversation [id=%s] updated with status \"%s\"",
                this.getConversationId(), status.getStatus()));
        return this;
    }

    boolean hasStatus(MessageStatus status) {
        return getMessageStatuses().stream()
                .anyMatch(ms -> ms.getStatus().equals(status.getStatus()) &&
                        Objects.equals(ms.getDescription(), status.getDescription()));
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

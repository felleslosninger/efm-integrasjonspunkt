package no.difi.meldingsutveksling.receipt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.MessageInformable;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.nextmove.AbstractEntity;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import org.hibernate.annotations.UpdateTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import static no.difi.meldingsutveksling.receipt.ConversationMarker.markerFrom;

@Getter
@Setter
@Entity
@Slf4j
@Table(name = "conversation",
        indexes = {@Index(columnList = "conversation_id")})
@ApiModel(description = "Conversation")
@NamedEntityGraph(name = "Conversation.messageStatuses", attributeNodes = @NamedAttributeNode("messageStatuses"))
public class Conversation extends AbstractEntity<Long> implements MessageInformable {

    public static final Logger statusLogger = LoggerFactory.getLogger("STATUS");

    @Override
    @JsonProperty
    @ApiModelProperty(
            position = 2,
            value = "Id",
            example = "1")
    public Long getId() {
        return super.getId();
    }

    @Column(name = "conversation_id")
    private String conversationId;
    @Column(name = "message_id")
    private String messageId;
    private String senderIdentifier;
    private String receiverIdentifier;
    private String messageReference;
    private String messageTitle;
    private String serviceCode;
    private String serviceEditionCode;
    @UpdateTimestamp
    @Setter(AccessLevel.PRIVATE)
    private OffsetDateTime lastUpdate;
    @JsonIgnore
    private boolean pollable;
    private boolean finished;
    private OffsetDateTime expiry;
    private ConversationDirection direction;
    private ServiceIdentifier serviceIdentifier;

    @OneToMany(mappedBy = "conversation", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MessageStatus> messageStatuses;

    public Conversation() {
    }

    private Conversation(String conversationId,
                         String messageId,
                         String messageReference,
                         String senderIdentifier,
                         String receiverIdentifier,
                         ConversationDirection direction,
                         String messageTitle,
                         ServiceIdentifier serviceIdentifier,
                         OffsetDateTime expiry,
                         OffsetDateTime lastUpdate
    ) {
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.messageReference = messageReference;
        this.senderIdentifier = senderIdentifier;
        this.receiverIdentifier = receiverIdentifier;
        this.direction = direction;
        this.messageTitle = messageTitle;
        this.messageStatuses = new HashSet<>();
        this.serviceIdentifier = serviceIdentifier;
        this.expiry = expiry;
        this.lastUpdate = lastUpdate;
    }

    private Conversation addMessageStatuses(MessageStatus... statuses) {
        if (statuses != null) {
            for (MessageStatus status : statuses) {
                addMessageStatus(status);
            }
        }
        return this;
    }

    public static Conversation of(String conversationId,
                                  String messageId,
                                  String messageReference,
                                  String senderIdentifier,
                                  String receiverIdentifier,
                                  ConversationDirection direction,
                                  String messageTitle,
                                  ServiceIdentifier serviceIdentifier,
                                  OffsetDateTime expiry,
                                  OffsetDateTime lastUpdate,
                                  MessageStatus... statuses) {
        return new Conversation(conversationId, messageId, messageReference, senderIdentifier, receiverIdentifier, direction, messageTitle, serviceIdentifier, expiry, lastUpdate)
                .addMessageStatuses(statuses);
    }

    public static Conversation of(MessageInformable msg, OffsetDateTime lastUpdate, MessageStatus... statuses) {
        return new Conversation(msg.getConversationId(), msg.getMessageId(), msg.getConversationId(), msg.getSenderIdentifier(), msg.getReceiverIdentifier(),
                msg.getDirection(), "", msg.getServiceIdentifier(), msg.getExpiry(), lastUpdate)
                .addMessageStatuses(statuses);
    }

    Conversation addMessageStatus(MessageStatus status) {
        status.setConversation(this);
        this.messageStatuses.add(status);
        if (statusLogger.isInfoEnabled()) {
            statusLogger.info(markerFrom(this).and(markerFrom(status)), String.format("Conversation [id=%s] updated with status \"%s\"",
                    this.getConversationId(), status.getStatus()));
        }
        return this;
    }

    @JsonIgnore
    boolean hasStatus(MessageStatus status) {
        return getMessageStatuses().stream()
                .anyMatch(ms -> ms.getStatus().equals(status.getStatus()));
    }

    @Override
    public String toString() {
        return "Conversation{" +
                "id=" + getId() + '\'' +
                ", conversationId='" + conversationId + '\'' +
                ", senderIdentifier='" + senderIdentifier + '\'' +
                ", receiverIdentifier='" + receiverIdentifier + '\'' +
                ", messageReference='" + messageReference + '\'' +
                ", messageTitle='" + messageTitle + '\'' +
                ", serviceCode='" + serviceCode + '\'' +
                ", serviceEditionCode='" + serviceEditionCode + '\'' +
                ", lastUpdate=" + lastUpdate +
                ", pollable=" + pollable +
                ", finished=" + finished +
                ", expiry=" + expiry +
                ", direction=" + direction +
                ", serviceIdentifier=" + serviceIdentifier +
                ", messageStatuses=" + messageStatuses +
                '}';
    }
}

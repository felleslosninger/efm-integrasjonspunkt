package no.difi.meldingsutveksling.status;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.MessageInformable;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.nextmove.AbstractEntity;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import static no.difi.meldingsutveksling.status.ConversationMarker.markerFrom;

@Getter
@Setter
@ToString
@Entity
@Slf4j
@Table(name = "conversation",
        indexes = {
                @Index(columnList = "conversation_id"),
                @Index(columnList = "message_id")
        })
@NamedEntityGraph(name = "Conversation.messageStatuses", attributeNodes = @NamedAttributeNode("messageStatuses"))
@DynamicUpdate
public class Conversation extends AbstractEntity<Long> {

    public static final Logger statusLogger = LoggerFactory.getLogger("STATUS");

    @Override
    @JsonProperty
    public Long getId() {
        return super.getId();
    }

    @Column(name = "conversation_id", length = 36)
    private String conversationId;
    @Column(name = "message_id", length = 36)
    private String messageId;
    private String sender;
    private String senderIdentifier;
    private String receiver;
    private String receiverIdentifier;
    private String processIdentifier;
    private String documentIdentifier;
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
    @ToString.Exclude
    private Set<MessageStatus> messageStatuses;

    public Conversation() {
    }

    private Conversation(String conversationId,
                         String messageId,
                         String messageReference,
                         PartnerIdentifier sender,
                         PartnerIdentifier receiver,
                         String processIdentifier,
                         String documentIdentifier,
                         ConversationDirection direction,
                         String messageTitle,
                         ServiceIdentifier serviceIdentifier,
                         OffsetDateTime expiry,
                         OffsetDateTime lastUpdate
    ) {
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.messageReference = messageReference;
        this.sender = sender.getIdentifier();
        this.senderIdentifier = sender.getPrimaryIdentifier();
        if (receiver != null) {
            this.receiver = receiver.getIdentifier();
            this.receiverIdentifier = receiver.getPrimaryIdentifier();
        }
        this.processIdentifier = processIdentifier;
        this.documentIdentifier = documentIdentifier;
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

    public static Conversation of(MessageInformable msg, OffsetDateTime lastUpdate, MessageStatus... statuses) {
        return new Conversation(msg.getConversationId(), msg.getMessageId(), msg.getConversationId(),
                msg.getSender(), msg.getReceiver(), msg.getProcessIdentifier(), msg.getDocumentIdentifier(),
                msg.getDirection(), "", msg.getServiceIdentifier(), msg.getExpiry(), lastUpdate)
                .addMessageStatuses(statuses);
    }

    Conversation addMessageStatus(MessageStatus status) {
        status.setConversation(this);
        this.messageStatuses.add(status);
        if (statusLogger.isInfoEnabled()) {
            statusLogger.info(markerFrom(this).and(markerFrom(status)), String.format("Message [id=%s] updated with status \"%s\"",
                    this.getMessageId(), status.getStatus()));
        }
        return this;
    }

    @JsonIgnore
    boolean hasStatus(MessageStatus status) {
        return getMessageStatuses().stream()
                .anyMatch(ms -> ms.getStatus().equals(status.getStatus()));
    }

}

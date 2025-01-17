package no.difi.meldingsutveksling.status;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import no.difi.meldingsutveksling.nextmove.AbstractEntity;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.view.Views;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Used for storing and tracking receipt information.
 */
@Getter
@Setter
@Entity
@Table(name = "message_status",
        indexes = {
            @Index(columnList = "conv_id")
        })
@NamedEntityGraph(name = "MessageStatus.conversation", attributeNodes = @NamedAttributeNode("conversation"))
public class MessageStatus extends AbstractEntity<Long> {

    @Override
    @JsonProperty
    @JsonView({Views.Conversation.class, Views.MessageStatus.class})
    public Long getId() {
        return super.getId();
    }

    @JsonProperty
    @JsonView(Views.MessageStatus.class)
    public Long getConvId() {
        return conversation.getId();
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conv_id")
    @JsonIgnore
    private Conversation conversation;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonView({Views.Conversation.class, Views.MessageStatus.class})
    private OffsetDateTime lastUpdate;
    @JsonView({Views.Conversation.class, Views.MessageStatus.class})
    private String status;
    @JsonView({Views.Conversation.class, Views.MessageStatus.class})
    private String description;

    @Lob
    @JsonView(Views.MessageStatus.class)
    private String rawReceipt;

    MessageStatus() {
    }

    @JsonView(Views.MessageStatus.class)
    public String getMessageId() {
        return conversation.getMessageId();
    }

    @JsonView(Views.MessageStatus.class)
    public String getConversationId() {
        return conversation.getConversationId();
    }

    private MessageStatus(String status, OffsetDateTime lastUpdate, String description) {
        this.status = status;
        this.lastUpdate = lastUpdate;
        this.description = description;
    }

    public static MessageStatus of(ReceiptStatus status, OffsetDateTime lastUpdate) {
        return new MessageStatus(status.toString(), lastUpdate, null);
    }

    public static MessageStatus of(ReceiptStatus status, OffsetDateTime lastUpdate, String description) {
        return new MessageStatus(status.toString(), lastUpdate, description);
    }

    @Override
    public String toString() {
        return "MessageStatus{" +
                "id=" + getId() +
                ", conversation=" + conversation +
                ", lastUpdate=" + lastUpdate +
                ", status='" + status + '\'' +
                ", description='" + description + '\'' +
                ", rawReceipt='" + rawReceipt + '\'' +
                '}';
    }
}

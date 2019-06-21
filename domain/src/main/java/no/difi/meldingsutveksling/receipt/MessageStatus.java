package no.difi.meldingsutveksling.receipt;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.base.MoreObjects;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import no.difi.meldingsutveksling.nextmove.AbstractEntity;
import no.difi.meldingsutveksling.view.Views;

import javax.persistence.*;
import java.time.OffsetDateTime;

/**
 * Used for storing and tracking receipt information.
 */
@Getter
@Setter
@Entity
@Table(name = "message_status")
@NamedEntityGraph(name = "MessageStatus.conversation", attributeNodes = @NamedAttributeNode("conversation"))
public class MessageStatus extends AbstractEntity<Long> {

    @Override
    @JsonProperty
    @ApiModelProperty(
            position = 2,
            value = "Id",
            example = "1")
    @JsonView({Views.Conversation.class, Views.MessageStatus.class})
    public Long getId() {
        return super.getId();
    }

    @JsonProperty
    @JsonView(Views.MessageStatus.class)
    public Long getConvId() {
        return conversation.getId();
    }

    @ManyToOne(fetch = FetchType.LAZY)
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
        return MoreObjects.toStringHelper(this)
                .add("id", getId())
                .add("lastUpdate", lastUpdate)
                .add("status", status)
                .add("description", description)
                .add("rawReceipt", rawReceipt)
                .toString();
    }
}

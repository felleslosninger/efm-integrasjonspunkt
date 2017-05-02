package no.difi.meldingsutveksling.receipt;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import lombok.Data;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.core.EDUCore;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Data
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
                         ServiceIdentifier serviceIdentifier,
                         List<MessageStatus> statuses) {
        this.conversationId = conversationId;
        this.messageReference = messageReference;
        this.receiverIdentifier = receiverIdentifier;
        this.messageTitle = messageTitle;
        this.messageStatuses = statuses;
        this.serviceIdentifier = serviceIdentifier;
        this.lastUpdate = LocalDateTime.now();
        switch (serviceIdentifier) {
            case DPV:
            case DPF:
                this.pollable = true;
                break;
            case DPI:
            case DPO:
            default:
                this.pollable = false;
                break;
        }
    }

    public static Conversation of(String conversationId,
                                  String messageReference,
                                  String receiverIdentifier,
                                  String messageTitle,
                                  ServiceIdentifier serviceIdentifier,
                                  MessageStatus... statuses) {
        if (statuses == null || statuses.length == 0) {
            return new Conversation(conversationId, messageReference, receiverIdentifier, messageTitle,
                    serviceIdentifier, Lists.newArrayList());
        }
        List<MessageStatus> statusList = Stream.of(statuses)
                .peek(r -> r.setConversationId(conversationId))
                .collect(Collectors.toList());
        return new Conversation(conversationId, messageReference, receiverIdentifier, messageTitle,
                serviceIdentifier, statusList);
    }

    public static Conversation of(EDUCore eduCore, MessageStatus... statuses) {
        String msgTitle = "";
        if (eduCore.getMessageType() == EDUCore.MessageType.EDU) {
            msgTitle = eduCore.getPayloadAsMeldingType().getJournpost().getJpInnhold();
        }
        List<MessageStatus> statusListList = Stream.of(statuses)
                .peek(r -> r.setConversationId(eduCore.getId()))
                .collect(Collectors.toList());
        return new Conversation(eduCore.getId(), eduCore.getMessageReference(), eduCore.getReceiver().getIdentifier(),
                msgTitle, eduCore.getServiceIdentifier() , statusListList);
    }

    public void addMessageStatus(MessageStatus status) {
        status.setConversationId(getConversationId());
        this.messageStatuses.add(status);
        this.lastUpdate = LocalDateTime.now();
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

package no.difi.meldingsutveksling.status;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.*;

import jakarta.annotation.Generated;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;


/**
 * QConversation is a Querydsl query type for Conversation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QConversation extends EntityPathBase<Conversation> {

    private static final long serialVersionUID = -690114889L;

    public static final QConversation conversation = new QConversation("conversation");

    public final no.difi.meldingsutveksling.nextmove.QAbstractEntity _super = new no.difi.meldingsutveksling.nextmove.QAbstractEntity(this);

    public final StringPath conversationId = createString("conversationId");

    public final EnumPath<no.difi.meldingsutveksling.nextmove.ConversationDirection> direction = createEnum("direction", no.difi.meldingsutveksling.nextmove.ConversationDirection.class);

    public final StringPath documentIdentifier = createString("documentIdentifier");

    public final DateTimePath<java.time.OffsetDateTime> expiry = createDateTime("expiry", java.time.OffsetDateTime.class);

    public final BooleanPath finished = createBoolean("finished");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.OffsetDateTime> lastUpdate = createDateTime("lastUpdate", java.time.OffsetDateTime.class);

    public final StringPath messageId = createString("messageId");

    public final StringPath messageReference = createString("messageReference");

    public final SetPath<MessageStatus, QMessageStatus> messageStatuses = this.<MessageStatus, QMessageStatus>createSet("messageStatuses", MessageStatus.class, QMessageStatus.class, PathInits.DIRECT2);

    public final StringPath messageTitle = createString("messageTitle");

    public final BooleanPath pollable = createBoolean("pollable");

    public final StringPath processIdentifier = createString("processIdentifier");

    public final StringPath receiver = createString("receiver");

    public final StringPath receiverIdentifier = createString("receiverIdentifier");

    public final StringPath sender = createString("sender");

    public final StringPath senderIdentifier = createString("senderIdentifier");

    public final StringPath serviceCode = createString("serviceCode");

    public final StringPath serviceEditionCode = createString("serviceEditionCode");

    public final EnumPath<no.difi.meldingsutveksling.ServiceIdentifier> serviceIdentifier = createEnum("serviceIdentifier", no.difi.meldingsutveksling.ServiceIdentifier.class);

    public QConversation(String variable) {
        super(Conversation.class, forVariable(variable));
    }

    public QConversation(Path<? extends Conversation> path) {
        super(path.getType(), path.getMetadata());
    }

    public QConversation(PathMetadata metadata) {
        super(Conversation.class, metadata);
    }

}


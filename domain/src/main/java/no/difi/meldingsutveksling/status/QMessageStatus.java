package no.difi.meldingsutveksling.status;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.*;

import jakarta.annotation.Generated;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;


/**
 * QMessageStatus is a Querydsl query type for MessageStatus
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMessageStatus extends EntityPathBase<MessageStatus> {

    private static final long serialVersionUID = 1348718373L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMessageStatus messageStatus = new QMessageStatus("messageStatus");

    public final no.difi.meldingsutveksling.nextmove.QAbstractEntity _super = new no.difi.meldingsutveksling.nextmove.QAbstractEntity(this);

    public final QConversation conversation;

    public final StringPath description = createString("description");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.OffsetDateTime> lastUpdate = createDateTime("lastUpdate", java.time.OffsetDateTime.class);

    public final StringPath rawReceipt = createString("rawReceipt");

    public final StringPath status = createString("status");

    public QMessageStatus(String variable) {
        this(MessageStatus.class, forVariable(variable), INITS);
    }

    public QMessageStatus(Path<? extends MessageStatus> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMessageStatus(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMessageStatus(PathMetadata metadata, PathInits inits) {
        this(MessageStatus.class, metadata, inits);
    }

    public QMessageStatus(Class<? extends MessageStatus> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.conversation = inits.isInitialized("conversation") ? new QConversation(forProperty("conversation")) : null;
    }

}


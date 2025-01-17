package no.difi.meldingsutveksling.nextmove;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.*;

import jakarta.annotation.Generated;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;


/**
 * QNextMoveMessage is a Querydsl query type for NextMoveMessage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNextMoveMessage extends EntityPathBase<NextMoveMessage> {

    private static final long serialVersionUID = 1767190177L;

    public static final QNextMoveMessage nextMoveMessage = new QNextMoveMessage("nextMoveMessage");

    public final QAbstractEntity _super = new QAbstractEntity(this);

    public final StringPath conversationId = createString("conversationId");

    public final SetPath<BusinessMessageFile, QBusinessMessageFile> files = this.<BusinessMessageFile, QBusinessMessageFile>createSet("files", BusinessMessageFile.class, QBusinessMessageFile.class, PathInits.DIRECT2);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.OffsetDateTime> lastUpdated = createDateTime("lastUpdated", java.time.OffsetDateTime.class);

    public final StringPath messageId = createString("messageId");

    public final StringPath processIdentifier = createString("processIdentifier");

    public final StringPath receiverIdentifier = createString("receiverIdentifier");

    public final SimplePath<no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument> sbd = createSimple("sbd", no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument.class);

    public final StringPath senderIdentifier = createString("senderIdentifier");

    public final EnumPath<no.difi.meldingsutveksling.ServiceIdentifier> serviceIdentifier = createEnum("serviceIdentifier", no.difi.meldingsutveksling.ServiceIdentifier.class);

    public QNextMoveMessage(String variable) {
        super(NextMoveMessage.class, forVariable(variable));
    }

    public QNextMoveMessage(Path<? extends NextMoveMessage> path) {
        super(path.getType(), path.getMetadata());
    }

    public QNextMoveMessage(PathMetadata metadata) {
        super(NextMoveMessage.class, metadata);
    }

}


package no.difi.meldingsutveksling.nextmove;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.*;

import jakarta.annotation.Generated;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;


/**
 * QNextMoveOutMessage is a Querydsl query type for NextMoveOutMessage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNextMoveOutMessage extends EntityPathBase<NextMoveOutMessage> {

    private static final long serialVersionUID = 1909442559L;

    public static final QNextMoveOutMessage nextMoveOutMessage = new QNextMoveOutMessage("nextMoveOutMessage");

    public final QNextMoveMessage _super = new QNextMoveMessage(this);

    //inherited
    public final StringPath conversationId = _super.conversationId;

    public final EnumPath<ConversationDirection> direction = createEnum("direction", ConversationDirection.class);

    //inherited
    public final SetPath<BusinessMessageFile, QBusinessMessageFile> files = _super.files;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final DateTimePath<java.time.OffsetDateTime> lastUpdated = _super.lastUpdated;

    //inherited
    public final StringPath messageId = _super.messageId;

    //inherited
    public final StringPath processIdentifier = _super.processIdentifier;

    //inherited
    public final StringPath receiverIdentifier = _super.receiverIdentifier;

    //inherited
    public final SimplePath<no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument> sbd = _super.sbd;

    //inherited
    public final StringPath senderIdentifier = _super.senderIdentifier;

    //inherited
    public final EnumPath<no.difi.meldingsutveksling.ServiceIdentifier> serviceIdentifier = _super.serviceIdentifier;

    public QNextMoveOutMessage(String variable) {
        super(NextMoveOutMessage.class, forVariable(variable));
    }

    public QNextMoveOutMessage(Path<? extends NextMoveOutMessage> path) {
        super(path.getType(), path.getMetadata());
    }

    public QNextMoveOutMessage(PathMetadata metadata) {
        super(NextMoveOutMessage.class, metadata);
    }

}


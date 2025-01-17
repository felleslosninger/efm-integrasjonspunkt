package no.difi.meldingsutveksling.nextmove;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.SimplePath;
import com.querydsl.core.types.dsl.StringPath;

import jakarta.annotation.Generated;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;


/**
 * QNextMoveMessageEntry is a Querydsl query type for NextMoveMessageEntry
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNextMoveMessageEntry extends EntityPathBase<NextMoveMessageEntry> {

    private static final long serialVersionUID = -1183586735L;

    public static final QNextMoveMessageEntry nextMoveMessageEntry = new QNextMoveMessageEntry("nextMoveMessageEntry");

    public final SimplePath<java.sql.Blob> content = createSimple("content", java.sql.Blob.class);

    public final NumberPath<Integer> entryId = createNumber("entryId", Integer.class);

    public final StringPath filename = createString("filename");

    public final StringPath messageId = createString("messageId");

    public final NumberPath<Long> size = createNumber("size", Long.class);

    public QNextMoveMessageEntry(String variable) {
        super(NextMoveMessageEntry.class, forVariable(variable));
    }

    public QNextMoveMessageEntry(Path<? extends NextMoveMessageEntry> path) {
        super(path.getType(), path.getMetadata());
    }

    public QNextMoveMessageEntry(PathMetadata metadata) {
        super(NextMoveMessageEntry.class, metadata);
    }

}


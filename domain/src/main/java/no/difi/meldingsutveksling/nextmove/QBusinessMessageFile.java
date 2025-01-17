package no.difi.meldingsutveksling.nextmove;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.*;

import jakarta.annotation.Generated;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;


/**
 * QBusinessMessageFile is a Querydsl query type for BusinessMessageFile
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBusinessMessageFile extends EntityPathBase<BusinessMessageFile> {

    private static final long serialVersionUID = 191158241L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBusinessMessageFile businessMessageFile = new QBusinessMessageFile("businessMessageFile");

    public final QAbstractEntity _super = new QAbstractEntity(this);

    public final NumberPath<Integer> dokumentnummer = createNumber("dokumentnummer", Integer.class);

    public final StringPath filename = createString("filename");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath identifier = createString("identifier");

    public final QNextMoveMessage message;

    public final StringPath mimetype = createString("mimetype");

    public final BooleanPath primaryDocument = createBoolean("primaryDocument");

    public final NumberPath<Long> size = createNumber("size", Long.class);

    public final StringPath title = createString("title");

    public QBusinessMessageFile(String variable) {
        this(BusinessMessageFile.class, forVariable(variable), INITS);
    }

    public QBusinessMessageFile(Path<? extends BusinessMessageFile> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBusinessMessageFile(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBusinessMessageFile(PathMetadata metadata, PathInits inits) {
        this(BusinessMessageFile.class, metadata, inits);
    }

    public QBusinessMessageFile(Class<? extends BusinessMessageFile> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.message = inits.isInitialized("message") ? new QNextMoveMessage(forProperty("message")) : null;
    }

}


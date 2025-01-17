package no.difi.meldingsutveksling.nextmove;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import jakarta.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QAbstractEntity is a Querydsl query type for AbstractEntity
 */
@Generated("com.querydsl.codegen.DefaultSupertypeSerializer")
public class QAbstractEntity extends EntityPathBase<AbstractEntity<? extends java.io.Serializable>> {

    private static final long serialVersionUID = 1135106631L;

    public static final QAbstractEntity abstractEntity = new QAbstractEntity("abstractEntity");

    public final SimplePath<java.io.Serializable> id = createSimple("id", java.io.Serializable.class);

    @SuppressWarnings({"all", "rawtypes", "unchecked"})
    public QAbstractEntity(String variable) {
        super((Class) AbstractEntity.class, forVariable(variable));
    }

    @SuppressWarnings({"all", "rawtypes", "unchecked"})
    public QAbstractEntity(Path<? extends AbstractEntity> path) {
        super((Class) path.getType(), path.getMetadata());
    }

    @SuppressWarnings({"all", "rawtypes", "unchecked"})
    public QAbstractEntity(PathMetadata metadata) {
        super((Class) AbstractEntity.class, metadata);
    }

}


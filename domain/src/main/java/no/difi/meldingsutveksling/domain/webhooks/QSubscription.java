package no.difi.meldingsutveksling.domain.webhooks;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;

import jakarta.annotation.Generated;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;


/**
 * QSubscription is a Querydsl query type for Subscription
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSubscription extends EntityPathBase<Subscription> {

    private static final long serialVersionUID = -787736371L;

    public static final QSubscription subscription = new QSubscription("subscription");

    public final no.difi.meldingsutveksling.nextmove.QAbstractEntity _super = new no.difi.meldingsutveksling.nextmove.QAbstractEntity(this);

    public final StringPath event = createString("event");

    public final StringPath filter = createString("filter");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final StringPath pushEndpoint = createString("pushEndpoint");

    public final StringPath resource = createString("resource");

    public QSubscription(String variable) {
        super(Subscription.class, forVariable(variable));
    }

    public QSubscription(Path<? extends Subscription> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSubscription(PathMetadata metadata) {
        super(Subscription.class, metadata);
    }

}


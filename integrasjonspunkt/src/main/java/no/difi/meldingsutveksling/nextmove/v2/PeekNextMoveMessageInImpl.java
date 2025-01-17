package no.difi.meldingsutveksling.nextmove.v2;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.nextmove.NextMoveInMessage;
import org.hibernate.CacheMode;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * EntityManager direkte for å unngå diverse caching og locking i Spring data
 * READ_UNCOMMITTED og cache disabled for at lock skal reflekteres i uthenta id-er så kjapt som mulig (og ikkje vente på heile lock-transaksjonen)
 * Hente opp til 20 kandidater for å tåle nokre kollisjoner på lock utan å måtte hente fleire fra database
 * Loop gjennom kandidater til ein fakisk får LOCK ok
 * ber i praksis konsument om å vente litt mha å returnere NO_CONTENT dersom ein ikkje får LOCK på nokon av kandidatane
 */
@Slf4j
public class PeekNextMoveMessageInImpl implements PeekNextMoveMessageIn {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
    public List<Long> findIdsForUnlockedMessages(NextMoveInMessageQueryInput input, int maxResults) {
        return em.createQuery(getQuery(input))
                .setMaxResults(maxResults)
                .setHint("org.hibernate.cacheMode", CacheMode.IGNORE)
                .setLockMode(LockModeType.NONE)
                .getResultList();
    }

    @Override
    @Transactional
    public Optional<NextMoveInMessage> lock(long id, OffsetDateTime lockTimeout) {
        int updateCount = em.createQuery("update NextMoveInMessage set lockTimeout = :lockTimeout " +
                "where id = :id and lockTimeout is null")
                .setParameter("lockTimeout", lockTimeout)
                .setParameter("id", id)
                .executeUpdate();

        if (updateCount == 1) {
            return Optional.ofNullable(getMessage(id));
        }

        return Optional.empty();
    }

    private NextMoveInMessage getMessage(long id) {
        return em.find(NextMoveInMessage.class, id);
    }

    private CriteriaQuery<Long> getQuery(NextMoveInMessageQueryInput input) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cr = cb.createQuery(Long.class);
        Root<NextMoveInMessage> root = cr.from(NextMoveInMessage.class);

        Predicate conjunction = cb.conjunction();

        if (input.getConversationId() != null) {
            conjunction = cb.and(conjunction, cb.equal(root.get("conversationId"), input.getConversationId()));
        }

        if (input.getMessageId() != null) {
            conjunction = cb.and(conjunction, cb.equal(root.get("messageId"), input.getMessageId()));
        }

        if (input.getReceiverIdentifier() != null) {
            conjunction = cb.and(conjunction, cb.equal(root.get("receiverIdentifier"), input.getReceiverIdentifier()));
        }

        if (input.getSenderIdentifier() != null) {
            conjunction = cb.and(conjunction, cb.equal(root.get("senderIdentifier"), input.getSenderIdentifier()));
        }

        if (input.getServiceIdentifier() != null) {
            conjunction = cb.and(conjunction, cb.equal(root.get("serviceIdentifier"), ServiceIdentifier.valueOf(input.getServiceIdentifier())));
        }

        if (input.getProcess() != null) {
            conjunction = cb.and(conjunction, cb.equal(root.get("processIdentifier"), input.getProcess()));
        }

        cr.select(root.get("id"))
                .where(cb.and(cb.isNull(root.get("lockTimeout")), conjunction))
                .orderBy(cb.asc(root.get("lastUpdated")));

        return cr;
    }
}

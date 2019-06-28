package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StandardBusinessDocumentRepository extends
        PagingAndSortingRepository<StandardBusinessDocument, Long>,
        QuerydslPredicateExecutor<StandardBusinessDocument> {
}

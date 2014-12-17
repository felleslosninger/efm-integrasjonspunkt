package no.difi.meldingsutveksling.adresseregister.repository;

import no.difi.meldingsutveksling.adresseregister.domain.VirksomhetsSertifikat;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

/**
 * Repository for AdresseRegisterRepository
 *
 * @author Glenn Bech
 */

@Repository
@RepositoryRestResource(collectionResourceRel = "certificates", path = "certificates")
public interface AdresseRegisterRepository extends PagingAndSortingRepository<VirksomhetsSertifikat, Integer> {
        VirksomhetsSertifikat findByorganizationNumber(@Param("organizationNumber") String organizationNumber);

}

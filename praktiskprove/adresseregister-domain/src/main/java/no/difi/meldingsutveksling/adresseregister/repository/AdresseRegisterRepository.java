package no.difi.meldingsutveksling.adresseregister.repository;

import no.difi.meldingsutveksling.adresseregister.domain.VirksomhetsSertifikat;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for AdresseRegisterRepository
 *
 * @author Glenn Bech
 */

@Repository
public interface AdresseRegisterRepository extends PagingAndSortingRepository<VirksomhetsSertifikat, String> {

    List<VirksomhetsSertifikat> findByOrgNumber(@Param("organizationNumber") String organizationNumber);

}

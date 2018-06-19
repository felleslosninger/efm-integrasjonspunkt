package no.difi.meldingsutveksling.serviceregistry;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SasKeyRepository extends CrudRepository<SasKeyWrapper, String> {
    List<SasKeyWrapper> findAll();
}

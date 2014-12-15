package no.difi.meldingsutveksling.adresseregister.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.difi.meldingsutveksling.adresseregister.domain.VirksomhetsSertifikat;
import no.difi.meldingsutveksling.adresseregister.repository.AdresseRegisterRepository;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.List;

/**
 * Loads some inital data whenever the application starts up
 *
 * @author Glenn Bech
 */
public class DataGenerator {

    public void load(ApplicationContext context) throws IOException {
        AdresseRegisterRepository repo = context.getBean(AdresseRegisterRepository.class);
        ObjectMapper mapper = new ObjectMapper();
        List<VirksomhetsSertifikat> initialData =
                mapper.readValue(getClass().getClassLoader().getResourceAsStream("testdata.json"), new TypeReference<List<VirksomhetsSertifikat>>() {
                });

        for (VirksomhetsSertifikat virksomhetsSertifikat : initialData) {
            repo.save(virksomhetsSertifikat);
        }
    }
}

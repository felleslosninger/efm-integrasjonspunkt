package no.difi.meldingsutveksling.adresseregister;

import no.difi.meldingsutveksling.adresseregister.domain.VirksomhetsSertifikat;
import no.difi.meldingsutveksling.adresseregister.repository.AdresseRegisterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @ Rest
 */

@RestController
@RequestMapping("/adresseregister")
public class AdresseRegisterController {

    private AddressRegister adressRegister = AdressRegisterFactory.createAdressRegister(); // step 1 mock

    @Autowired
    private AdresseRegisterRepository repository;

    @RequestMapping(value = "/{orgNr}/crt", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public CertificateResponse getPublicKey(@PathVariable(value = "orgNr") String orgNr) {
        CertificateResponse response = new CertificateResponse();
        response.setKnutepuntHostName("http://muv-knutepunkt.herokuapp.com/noarkExchange");
        response.setBase64EncondedCertificate(adressRegister.getCeritifcateString(orgNr));
        return response;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<VirksomhetsSertifikat> get() {
        PageRequest page = new PageRequest(0, 10);
        Page<VirksomhetsSertifikat> firstPage = repository.findAll(page);
        return firstPage.getContent();
    }

    public AddressRegister getAdressRegister() {
        return adressRegister;
    }

    public void setAdressRegister(AddressRegister adressRegister) {
        this.adressRegister = adressRegister;
    }

    public AdresseRegisterRepository getRepository() {
        return repository;
    }

    public void setRepository(AdresseRegisterRepository repository) {
        this.repository = repository;
    }
}

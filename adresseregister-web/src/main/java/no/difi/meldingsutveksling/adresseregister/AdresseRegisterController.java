package no.difi.meldingsutveksling.adresseregister;

import no.difi.meldingsutveksling.adresseregister.domain.VirksomhetsSertifikat;
import no.difi.meldingsutveksling.adresseregister.repository.AdresseRegisterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ Rest
 */

@RestController
@RequestMapping("/adresseregister")
public class AdresseRegisterController {

    @Autowired
    AdresseRegisterRepository repository;

    @RequestMapping(value = "/{orgNr}/crt", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public CertificateResponse getCertificate(@PathVariable(value = "orgNr") String orgNr) {
        CertificateResponse response = new CertificateResponse();
        VirksomhetsSertifikat sertifikat = repository.findByorganizationNumber(orgNr);
        if (sertifikat == null) {
            throw new ResourceNotFoundException();
        }
        response.setBase64EncondedCertificate(sertifikat.getPem());
        return response;
    }


    @RequestMapping(value = "/{orgNr}/crt", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_VALUE})
    public void putCertificate(@PathVariable(value = "orgNr") String orgNr, String pemString) {
        CertificateResponse response = new CertificateResponse();
        response.setBase64EncondedCertificate(pemString);
        VirksomhetsSertifikat sertifikat = new VirksomhetsSertifikat();
        sertifikat.setActive(true);
        sertifikat.setOrganizationNumber(orgNr);
        sertifikat.setPem(pemString);
        repository.save(sertifikat);
    }

}

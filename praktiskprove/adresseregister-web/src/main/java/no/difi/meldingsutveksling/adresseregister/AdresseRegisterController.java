package no.difi.meldingsutveksling.adresseregister;

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

    private AddressRegister adressRegister = AdressRegisterFactory.createAdressRegister(); // step 1 mock

    @RequestMapping(value = "/{orgNr}/crt", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public CertificateResponse getPublicKey(@PathVariable(value = "orgNr") String orgNr) {
        CertificateResponse response = new CertificateResponse();
        response.setKnutepuntHostName("http://muv-knutepunkt.herokuapp.com/noarkExchange");
        response.setBase64EncondedCertificate(adressRegister.getCeritifcateString(orgNr));
        return response;
    }

    public AddressRegister getAdressRegister() {
        return adressRegister;
    }

    public void setAdressRegister(AddressRegister adressRegister) {
        this.adressRegister = adressRegister;
    }

}

package no.difi.meldingsutveksling.adresseregister.client;

import org.springframework.stereotype.Component;
import retrofit.http.GET;

/**
 * @author Glenn Bech
 */

@Component

public class AdresseRegisterClient {


    public String getCertificate(String orgNr) {

    }

    public String getPublicKey(String orgNr) {

    }


    interface IAdresseRegister {

        @GET("/adresseregister/{orgNr}/cert")
        String getCertificate(String orgNr);

        @GET("/adresseregister/{orgNr}/cert")
        String getPublicKey(String orgNr);
    }
}

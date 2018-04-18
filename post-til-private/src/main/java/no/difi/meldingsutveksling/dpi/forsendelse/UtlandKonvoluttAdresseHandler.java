package no.difi.meldingsutveksling.dpi.forsendelse;

import no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress;
import no.difi.sdp.client2.domain.fysisk_post.KonvoluttAdresse;

public class UtlandKonvoluttAdresseHandler implements KonvoluttAdresseHandler {
    @Override
    public KonvoluttAdresse handle(PostAddress postAddress) {
        return KonvoluttAdresse.build(postAddress.getName()).iUtlandet(postAddress.getStreet(), null,
                null, null, postAddress.getCountry()).build();
    }
}

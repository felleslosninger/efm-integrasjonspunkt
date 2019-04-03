package no.difi.meldingsutveksling.dpi.forsendelse;

import no.difi.meldingsutveksling.nextmove.PostAddress;
import no.difi.sdp.client2.domain.fysisk_post.KonvoluttAdresse;

public class UtlandKonvoluttAdresseHandler implements KonvoluttAdresseHandler {
    @Override
    public KonvoluttAdresse handle(PostAddress postAddress) {
        return KonvoluttAdresse.build(postAddress.getName())
                .iUtlandet(postAddress.getAddressLine1(),
                        postAddress.getAddressLine2(),
                        postAddress.getAddressLine3(),
                        postAddress.getAddressLine4(),
                        postAddress.getCountry()).build();
    }
}

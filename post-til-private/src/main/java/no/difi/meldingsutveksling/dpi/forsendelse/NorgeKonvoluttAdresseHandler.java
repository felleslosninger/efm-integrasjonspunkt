package no.difi.meldingsutveksling.dpi.forsendelse;

import no.difi.meldingsutveksling.nextmove.PostAddress;
import no.difi.sdp.client2.domain.fysisk_post.KonvoluttAdresse;

public class NorgeKonvoluttAdresseHandler implements KonvoluttAdresseHandler {
    @Override
    public KonvoluttAdresse handle(PostAddress postAddress) {
        return KonvoluttAdresse.build(postAddress.getName())
                .iNorge(postAddress.getAddressLine1(),
                        postAddress.getAddressLine2(),
                        postAddress.getAddressLine3(),
                        postAddress.getPostalCode(),
                        postAddress.getPostalArea()).build();
    }
}

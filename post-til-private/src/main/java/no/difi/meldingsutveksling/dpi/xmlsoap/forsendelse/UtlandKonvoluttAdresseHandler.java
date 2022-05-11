package no.difi.meldingsutveksling.dpi.xmlsoap.forsendelse;

import no.difi.meldingsutveksling.nextmove.PostAddress;
import no.difi.sdp.client2.domain.fysisk_post.KonvoluttAdresse;

public class UtlandKonvoluttAdresseHandler implements KonvoluttAdresseHandler {
    @Override
    public KonvoluttAdresse handle(PostAddress postAddress) {
        return KonvoluttAdresse.build(postAddress.getNavn())
                .iUtlandet(postAddress.getAdresselinje1(),
                        postAddress.getAdresselinje2(),
                        postAddress.getAdresselinje3(),
                        postAddress.getAdresselinje4(),
                        postAddress.getLand()).build();
    }
}

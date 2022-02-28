package no.difi.meldingsutveksling.dpi.xmlsoap.forsendelse;

import no.difi.meldingsutveksling.nextmove.PostAddress;
import no.difi.sdp.client2.domain.fysisk_post.KonvoluttAdresse;

interface KonvoluttAdresseHandler {
    KonvoluttAdresse handle(PostAddress postAddress);
}

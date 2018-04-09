package no.difi.meldingsutveksling.dpi.forsendelse;

import no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress;
import no.difi.sdp.client2.domain.fysisk_post.KonvoluttAdresse;

interface KonvoluttAdresseHandler {
    KonvoluttAdresse handle(PostAddress postAddress);
}

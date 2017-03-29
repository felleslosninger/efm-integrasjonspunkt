package no.difi.meldingsutveksling.dpi.forsendelse;

import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress;
import no.difi.sdp.client2.domain.fysisk_post.KonvoluttAdresse;

public class NorgeKonvoluttAdresseHandler implements KonvoluttAdresseHandler {
    @Override
    public KonvoluttAdresse handle(MeldingsformidlerRequest request) {
        PostAddress postAddress = request.getPostAddress();
        return KonvoluttAdresse.build(postAddress.getName()).iNorge(postAddress.getStreet(), null,
                null, postAddress.getPostalCode(), postAddress.getPostalArea()).build();
    }
}

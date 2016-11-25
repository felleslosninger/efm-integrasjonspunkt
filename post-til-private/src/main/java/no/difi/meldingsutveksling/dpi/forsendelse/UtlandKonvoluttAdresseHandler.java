package no.difi.meldingsutveksling.dpi.forsendelse;

import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress;
import no.difi.sdp.client2.domain.fysisk_post.KonvoluttAdresse;

public class UtlandKonvoluttAdresseHandler implements KonvoluttAdresseHandler {
    @Override
    public KonvoluttAdresse handle(MeldingsformidlerRequest request) {
        PostAddress postAddress = request.getPostAddress();
        return KonvoluttAdresse.build(postAddress.getName()).iUtlandet(postAddress.getStreet1(), postAddress.getStreet2(), postAddress.getStreet3(), postAddress.getStreet4(), postAddress.getCountry()).build();
    }
}

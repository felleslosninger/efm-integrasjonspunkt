package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.domain.capabilities.PostalAddress;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress;
import org.springframework.stereotype.Component;

@Component
public class PostalAddressFactory {
    PostalAddress getPostalAddress(PostAddress in) {
        if (in == null) {
            return null;
        }

        return new PostalAddress()
                .setName(in.getName())
                .setStreet(in.getStreet())
                .setPostalCode(in.getPostalCode())
                .setPostalArea(in.getPostalArea())
                .setCountry(in.getCountry());
    }
}

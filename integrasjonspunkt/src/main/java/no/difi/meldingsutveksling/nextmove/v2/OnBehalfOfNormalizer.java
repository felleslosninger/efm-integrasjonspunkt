package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnBehalfOfNormalizer {

    private final IntegrasjonspunktProperties properties;

    public void normalize(StandardBusinessDocument sbd) {
        PartnerIdentifier sender = sbd.getSenderIdentifier();

        if (sender == null) {
            return;
        }

        sender.as(Iso6523.class).ifPresent(iso6523 -> {
            if (iso6523.hasOrganizationPartIdentifier()) {
                return;
            }

            if (iso6523.getOrganizationIdentifier().equals(properties.getOrg().getNumber())) {
                return;
            }

            sbd.setSenderIdentifier(Iso6523.of(iso6523.getIcd(), properties.getOrg().getNumber(), iso6523.getOrganizationIdentifier()));
        });
    }
}

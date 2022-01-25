package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnBehalfOfNormalizer {

    private final IntegrasjonspunktProperties properties;

    public void normalize(StandardBusinessDocument sbd) {
        Organisasjonsnummer sender = SBDUtil.getSender(sbd);

        if (sender == null) {
            return;
        }

        if (sender.hasPaaVegneAvOrgnr()) {
            return;
        }

        if (sender.getOrgNummer().equals(properties.getOrg().getNumber())) {
            return;
        }

        StandardBusinessDocumentUtils.getFirstSenderIdentifier(sbd)
                .ifPresent(p -> {
                    Iso6523 onBehalfOf = Iso6523.parse(p.getValue());
                    Iso6523 combined = onBehalfOf.withOrganizationPartIdentifier(onBehalfOf.getOrganizationIdentifier())
                            .withOrganizationIdentifier(properties.getOrg().getNumber());
                    p.setValue(combined.toString());
                });
    }
}

package no.difi.meldingsutveksling.domain.sbdh;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SBDService {

    private final Clock clock;
    private final IntegrasjonspunktProperties properties;

    public boolean isExpired(StandardBusinessDocument sbd) {
        return StandardBusinessDocumentUtils.getExpectedResponseDateTime(sbd)
                .map(this::isExpired)
                .orElse(false);
    }

    private boolean isExpired(OffsetDateTime expectedResponseDateTime) {
        OffsetDateTime currentDateTime = OffsetDateTime.now(clock);
        return currentDateTime.isAfter(expectedResponseDateTime);
    }

    public String getSenderIdentifier(StandardBusinessDocument sbd) {
        return getSender(sbd).getOrgNummer();
    }

    public Optional<String> getOnBehalfOfOrgNr(StandardBusinessDocument sbd) {
        return getSender(sbd).getPaaVegneAvOrgnr();
    }

    public Organisasjonsnummer getSender(StandardBusinessDocument sbd) {
        return parseSender(SBDUtil.getSender(sbd), SBDUtil.getReceiver(sbd));
    }

    public Organisasjonsnummer parseSender(String sender, String receiver) {
        Organisasjonsnummer org = Organisasjonsnummer.parse(sender);

        if (org.hasOnBehalfOf()) {
            return org;
        }

        if(Organisasjonsnummer.parse(receiver).getOrgNummer().equals(properties.getOrg().getNumber())) {
            return org;
        }

        if (org.getOrgNummer().equals(properties.getOrg().getNumber())) {
            return org;
        }

        return Organisasjonsnummer.from(properties.getOrg().getNumber(), org.getOrgNummer());
    }

    public String getReceiverIdentifier(StandardBusinessDocument sbd) {
        return getReceiver(sbd).getOrgNummer();
    }

    public Organisasjonsnummer getReceiver(StandardBusinessDocument sbd) {
        return parseReceiver(SBDUtil.getReceiver(sbd));
    }

    public Organisasjonsnummer parseReceiver(String receiver) {
        return Organisasjonsnummer.parse(receiver);
    }
}

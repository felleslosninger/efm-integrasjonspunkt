package no.difi.meldingsutveksling.dph;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.dph.client.DphClient;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import org.springframework.boot.CommandLineRunner;

import java.text.ParseException;

@Slf4j
@RequiredArgsConstructor
public class DphAppStartupRunner implements CommandLineRunner {

    private final DphClient dphClient;
    private final IntegrasjonspunktProperties properties;
    private final ServiceRegistryLookup serviceRegistryLookup;

    @Override
    public void run(String... args) throws ParseException {
        for (Integer herId : properties.getDph().getHerIds()) {
            log.info("DPH - Verifying HER-id = {}", herId);
            NhnIdentifier identifier = NhnIdentifier.herId(herId);
            InfoRecord infoRecord = serviceRegistryLookup.getInfoRecord(identifier.getIdentifier());
            log.info("DPH - ServiceRegistry InfoRecord = {}", infoRecord);
            log.info("DPH - Verifying that {} has Altinn delegation for the [{}] scope(s) on behalf of {}",
                properties.getOrg().getNumber(),
                String.join(", ", properties.getDph().getOidc().getScopes()),
                infoRecord.getOrganizationNumber());
            Iso6523 onBehalfOf = Iso6523.of(ICD.NO_ORG, infoRecord.getOrganizationNumber());
            String token = dphClient.getMaskinportenToken(onBehalfOf);
            JWT jwt = JWTParser.parse(token);
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            log.info("DPH - Access token claims = {}", claims);
            log.info("DPH - Trying to fetch messages for HER-id = {}", herId);
            dphClient.getMessages(onBehalfOf, herId);
            log.info("DPH - HER-id = {} verified successfully", herId);
        }
    }
}

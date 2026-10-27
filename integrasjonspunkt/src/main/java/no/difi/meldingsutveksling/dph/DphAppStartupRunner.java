package no.difi.meldingsutveksling.dph;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.DphProperties;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.dph.client.DphClientService;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.text.ParseException;

@Slf4j
@RequiredArgsConstructor
public class DphAppStartupRunner implements CommandLineRunner {

    private final DphClientService dphClientService;
    private final IntegrasjonspunktProperties properties;
    private final ServiceRegistryLookup serviceRegistryLookup;

    @Override
    public void run(String... args) throws ParseException {
        DphProperties.Startup.OnFail onFail = properties.getDph().getStartup().getOnfail();
        for (Integer herId : properties.getDph().getHerIds()) {
            try {
                verify(herId);
            } catch (Exception e) {
                if (onFail == DphProperties.Startup.OnFail.STOP) {
                    log.error("DPH - Verification of HER-id = {} failed, stopping application (set difi.move.dph.startup.onfail=log to only log the error). Reason: {}",
                        herId, getReason(e));
                    throw e;
                }
                log.error("DPH - Verification of HER-id = {} failed. Reason: {}", herId, getReason(e), e);
            }
        }
    }

    private void verify(Integer herId) throws ParseException {
        log.info("DPH - Verifying HER-id = {}", herId);
        NhnIdentifier identifier = NhnIdentifier.herId(herId);
        InfoRecord infoRecord = serviceRegistryLookup.getInfoRecord(identifier.getIdentifier());
        log.info("DPH - ServiceRegistry InfoRecord = {}", infoRecord);
        log.info("DPH - Verifying that {} has Altinn delegation for the [{}] scope(s) on behalf of {}",
            properties.getOrg().getNumber(),
            String.join(", ", properties.getDph().getOidc().getScopes()),
            infoRecord.getOrganizationNumber());
        Iso6523 onBehalfOf = Iso6523.of(ICD.NO_ORG, infoRecord.getOrganizationNumber());
        String token = dphClientService.getMaskinportenToken(onBehalfOf);
        JWT jwt = JWTParser.parse(token);
        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        log.info("DPH - Access token claims = {}", claims);
        log.info("DPH - Trying to fetch messages for HER-id = {}", herId);
        dphClientService.getMessages(onBehalfOf, herId);
        log.info("DPH - HER-id = {} verified successfully", herId);
    }

    /**
     * Includes the response body for HTTP errors, since that is where e.g. Maskinporten puts its error code (MP-250 etc.).
     */
    static String getReason(Throwable t) {
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(t);
        if (cause instanceof RestClientResponseException e) {
            return e.getStatusCode() + " " + e.getResponseBodyAsString();
        }
        if (cause instanceof WebClientResponseException e) {
            return e.getStatusCode() + " " + e.getResponseBodyAsString();
        }
        return cause.toString();
    }
}

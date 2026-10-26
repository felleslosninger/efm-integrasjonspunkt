package no.difi.meldingsutveksling.dph;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.difi.meldingsutveksling.config.DphProperties;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.dph.client.DphClientService;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DphAppStartupRunnerTest {

    private static final Integer FAILING_HER_ID = 1111;
    private static final Integer OK_HER_ID = 2222;
    private static final String MP_250_BODY = "{\"error\":\"invalid_grant\",\"error_description\":\"MP-250: Consumer has not delegated access to the scope\"}";

    @Mock
    private DphClientService dphClientService;
    @Mock
    private ServiceRegistryLookup serviceRegistryLookup;

    private IntegrasjonspunktProperties properties;
    private DphAppStartupRunner target;

    @BeforeEach
    public void setUp() {
        DphProperties dph = new DphProperties();
        dph.setHerIds(List.of(FAILING_HER_ID, OK_HER_ID));
        dph.setOidc(new DphProperties.Oidc().setScopes(List.of("eformidling:dph")));
        properties = new IntegrasjonspunktProperties()
            .setOrg(new IntegrasjonspunktProperties.Organization().setNumber("987464291"))
            .setDph(dph);
        target = new DphAppStartupRunner(dphClientService, properties, serviceRegistryLookup);

        lenient().when(serviceRegistryLookup.getInfoRecord(anyString())).thenReturn(new InfoRecord().setOrganizationNumber("910076787"));
        lenient().when(dphClientService.getMaskinportenToken(any(Iso6523.class)))
            .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
                MP_250_BODY.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8))
            .thenReturn(new PlainJWT(new JWTClaimsSet.Builder().subject("test").build()).serialize());
    }

    @Test
    public void stop_is_default_and_rethrows_on_first_failure() {
        assertEquals(DphProperties.Startup.OnFail.STOP, properties.getDph().getStartup().getOnfail());

        assertThrows(HttpClientErrorException.class, () -> target.run());

        verify(dphClientService, times(1)).getMaskinportenToken(any());
        verify(dphClientService, never()).getMessages(any(), any());
    }

    @Test
    public void log_continues_with_remaining_her_ids() {
        properties.getDph().getStartup().setOnfail(DphProperties.Startup.OnFail.LOG);

        assertDoesNotThrow(() -> target.run());

        verify(dphClientService, never()).getMessages(any(), eq(FAILING_HER_ID));
        verify(dphClientService).getMessages(any(), eq(OK_HER_ID));
    }

    @Test
    public void reason_includes_maskinporten_error_body() {
        HttpClientErrorException e = HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
            MP_250_BODY.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        String reason = DphAppStartupRunner.getReason(new RuntimeException("wrapped", e));

        assertTrue(reason.contains("400"), reason);
        assertTrue(reason.contains("MP-250"), reason);
    }

    @Test
    public void onfail_property_binds_from_lowercase_value() {
        DphProperties.Startup startup = new Binder(new MapConfigurationPropertySource(Map.of("difi.move.dph.startup.onfail", "log")))
            .bind("difi.move.dph.startup", DphProperties.Startup.class)
            .get();

        assertEquals(DphProperties.Startup.OnFail.LOG, startup.getOnfail());
    }
}

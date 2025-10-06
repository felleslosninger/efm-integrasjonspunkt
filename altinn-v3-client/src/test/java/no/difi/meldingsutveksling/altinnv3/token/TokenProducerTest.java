package no.difi.meldingsutveksling.altinnv3.token;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.config.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * This test verifies that both TokenService and TokenExchangeService have been
 * called the same number of times that the produceToken() is called and that
 * the Qualifiers work as expected.
 */
@SpringBootTest(classes = {
    DpoTokenProducer.class,
    DpvTokenProducer.class
})
class TokenProducerTest {

    @Inject
    @Qualifier("DpoTokenProducer")
    TokenProducer dpoTokenProducer;

    @Inject
    @Qualifier("DpvTokenProducer")
    TokenProducer dpvTokenProducer;

    @MockitoBean
    IntegrasjonspunktProperties integrasjonspunktProperties;

    @MockitoBean
    TokenService tokenService;

    @MockitoBean
    TokenExchangeService tokenExchangeService;

    @BeforeEach
    void setUp() {

        var now = LocalDateTime.parse("2025-10-06T08:02:52.259628");

        var dpo = new AltinnFormidlingsTjenestenConfig();
        var oidc = new Oidc();
        oidc.setAuthenticationType(AuthenticationType.CERTIFICATE);
        dpo.setOidc(oidc);
        dpo.setAuthorizationDetails(new AltinnAuthorizationDetails());
        when(integrasjonspunktProperties.getDpo()).thenReturn(dpo);
        when(integrasjonspunktProperties.getDpv()).thenReturn(new PostVirksomheter());
        when(tokenService.fetchToken(any(), any(), any())).thenReturn("maskinportentoken");
        when(tokenExchangeService.exchangeToken(any(), any())).thenReturn("altinntoken");
    }

    @Test
    void produceDpoToken() {
        Mockito.clearInvocations(tokenService);
        Mockito.clearInvocations(tokenExchangeService);
        var scopes = List.of("scopes");
        var token = dpoTokenProducer.produceToken(scopes);
        dpoTokenProducer.produceToken(scopes);
        dpoTokenProducer.produceToken(scopes);
        assertEquals("altinntoken", token);
        verify(tokenService, times(3)).fetchToken(any(), any(), any());
        verify(tokenExchangeService, times(3)).exchangeToken(any(), any());
    }

    @Test
    void produceDpvToken() {
        Mockito.clearInvocations(tokenService);
        Mockito.clearInvocations(tokenExchangeService);
        var scopes = List.of("scopes");
        var token = dpvTokenProducer.produceToken(scopes);
        dpvTokenProducer.produceToken(scopes);
        dpvTokenProducer.produceToken(scopes);
        assertEquals("altinntoken", token);
        verify(tokenService, times(3)).fetchToken(any(), any(), any());
        verify(tokenExchangeService, times(3)).exchangeToken(any(), any());
    }

}

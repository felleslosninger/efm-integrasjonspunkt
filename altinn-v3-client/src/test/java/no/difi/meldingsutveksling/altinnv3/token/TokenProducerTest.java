package no.difi.meldingsutveksling.altinnv3.token;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.config.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
    DpoTokenProducer dpoTokenProducer;

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
        var dpo = new AltinnFormidlingsTjenestenConfig();
        var oidc = new Oidc();
        oidc.setAuthenticationType(AuthenticationType.CERTIFICATE);
        dpo.setOidc(oidc);
        dpo.setSystemUser(new AltinnSystemUser());
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
        var token = dpoTokenProducer.produceToken(integrasjonspunktProperties.getDpo().getSystemUser(), scopes);
        dpoTokenProducer.produceToken(integrasjonspunktProperties.getDpo().getSystemUser(), scopes);
        dpoTokenProducer.produceToken(integrasjonspunktProperties.getDpo().getSystemUser(), scopes);
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

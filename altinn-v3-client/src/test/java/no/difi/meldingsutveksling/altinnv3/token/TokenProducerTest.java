package no.difi.meldingsutveksling.altinnv3.token;

import no.difi.meldingsutveksling.config.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * This test verifies that both TokenService and TokenExchangeService have been
 * called the same number of times that the produceToken() is called and that
 * the Qualifiers work as expected.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokenProducerTest {

    @InjectMocks
    DpoTokenProducer dpoTokenProducer;

    @InjectMocks
    DpvTokenProducer dpvTokenProducer;

    @Mock
    IntegrasjonspunktProperties integrasjonspunktProperties;

    @Mock
    TokenService tokenService;

    @Mock
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

package no.difi.meldingsutveksling.ptp;

import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.digital_post.EpostVarsel;
import org.junit.Before;
import org.junit.Test;

import java.security.KeyStoreException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class EmailNotificationDigitalPostBuilderHandlerTest {

    private MeldingsformidlerClient.Config config;
    private DigitalPost.Builder builder;

    @Before
    public void setup() {
        config = mock(MeldingsformidlerClient.Config.class);
        builder = mock(DigitalPost.Builder.class);
    }

    @Test
    public void emailFeatureDisabledShouldNotSetEpostVarsel() throws KeyStoreException {
        when(config.isEnableEmail()).thenReturn(false);

        EmailNotificationDigitalPostBuilderHandler handler = new EmailNotificationDigitalPostBuilderHandler(config);
        builder = handler.handle(new Request().withNotifiable(true), builder);

        verify(builder, never()).epostVarsel(any(EpostVarsel.class));
    }

    @Test
    public void notifiableFalseShouldNotSetEpostVarsel() throws KeyStoreException {
        when(config.isEnableEmail()).thenReturn(true);

        EmailNotificationDigitalPostBuilderHandler handler = new EmailNotificationDigitalPostBuilderHandler(config);
        builder = handler.handle(new Request().withNotifiable(false), builder);

        verify(builder, never()).epostVarsel(any(EpostVarsel.class));
    }

    @Test
    public void notifiableAndFeatureEnabledShouldAdEpostVarsel() throws KeyStoreException {
        when(config.isEnableEmail()).thenReturn(true);

        EmailNotificationDigitalPostBuilderHandler handler = new EmailNotificationDigitalPostBuilderHandler(config);
        builder = handler.handle(new Request().withNotifiable(true), builder);

        verify(builder).epostVarsel(any(EpostVarsel.class));
    }

}
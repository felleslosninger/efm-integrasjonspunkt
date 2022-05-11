package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dpi.xmlsoap.EmailNotificationDigitalPostBuilderHandler;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.digital_post.EpostVarsel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class EmailNotificationDigitalPostBuilderHandlerTest {

    private DigitalPostInnbyggerConfig config;
    private DigitalPost.Builder builder;

    @BeforeEach
    public void setup() {
        config = mock(DigitalPostInnbyggerConfig.class);
        builder = mock(DigitalPost.Builder.class);
    }

    @Test
    public void emailFeatureDisabledShouldNotSetEpostVarsel() {
        EmailNotificationDigitalPostBuilderHandler handler = new EmailNotificationDigitalPostBuilderHandler(config);
        builder = handler.handle(MeldingsformidlerRequest.builder().notifiable(true).build(), builder);

        verify(builder, never()).epostVarsel(any(EpostVarsel.class));
    }

    @Test
    public void notifiableFalseShouldNotSetEpostVarsel() {
        EmailNotificationDigitalPostBuilderHandler handler = new EmailNotificationDigitalPostBuilderHandler(config);
        builder = handler.handle(MeldingsformidlerRequest.builder().notifiable(false).build(), builder);

        verify(builder, never()).epostVarsel(any(EpostVarsel.class));
    }

    @Test
    public void notifiableAndFeatureEnabledWithEmailShouldAddEpostVarsel() {
        EmailNotificationDigitalPostBuilderHandler handler = new EmailNotificationDigitalPostBuilderHandler(config);
        builder = handler.handle(MeldingsformidlerRequest.builder()
                .notifiable(true)
                .emailAddress("foo@foo.com")
                .emailVarslingstekst("Notification")
                .build(), builder);

        verify(builder).epostVarsel(any(EpostVarsel.class));
    }

    @Test
    public void notifiableAndFeatureEnabledWithoutEmailShouldNotAddEpostVarsel() {
        EmailNotificationDigitalPostBuilderHandler handler = new EmailNotificationDigitalPostBuilderHandler(config);
        builder = handler.handle(MeldingsformidlerRequest.builder().notifiable(true).build(), builder);

        verify(builder, never()).epostVarsel(any(EpostVarsel.class));
    }
}
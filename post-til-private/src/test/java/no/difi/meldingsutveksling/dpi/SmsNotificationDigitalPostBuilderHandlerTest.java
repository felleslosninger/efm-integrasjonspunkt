package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dpi.xmlsoap.SmsNotificationDigitalPostBuilderHandler;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.digital_post.SmsVarsel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class SmsNotificationDigitalPostBuilderHandlerTest {

    private DigitalPostInnbyggerConfig config;
    private DigitalPost.Builder builder;

    @BeforeEach
    public void setup() {
        config = mock(DigitalPostInnbyggerConfig.class);
        builder = mock(DigitalPost.Builder.class);
    }

    @Test
    public void smsNotificationDisabledShouldNotSetEpostVarsel() {
        SmsNotificationDigitalPostBuilderHandler handler = new SmsNotificationDigitalPostBuilderHandler(config);
        handler.handle(MeldingsformidlerRequest.builder()
                .notifiable(true)
                .emailAddress("foo")
                .build(), builder);

        verify(builder, never()).smsVarsel(any(SmsVarsel.class));
    }

    @Test
    public void notifiableFalseShouldNotSetSmsVarsel() {
        SmsNotificationDigitalPostBuilderHandler handler = new SmsNotificationDigitalPostBuilderHandler(config);
        handler.handle(MeldingsformidlerRequest.builder().notifiable(false).build(), builder);

        verify(builder, never()).smsVarsel(any(SmsVarsel.class));
    }

    @Test
    public void notifiableAndSmsFeatureEnabledWithoutNumberShouldNotSetSmsVarsel() {
        SmsNotificationDigitalPostBuilderHandler handler = new SmsNotificationDigitalPostBuilderHandler(config);
        handler.handle(MeldingsformidlerRequest.builder().notifiable(true).build(), builder);

        verify(builder, never()).smsVarsel(any(SmsVarsel.class));
    }

    @Test
    public void notifiableAndSmsFeatureEnabledWithNumberShouldSetSmsVarsel() {
        SmsNotificationDigitalPostBuilderHandler handler = new SmsNotificationDigitalPostBuilderHandler(config);
        handler.handle(MeldingsformidlerRequest.builder().notifiable(true).mobileNumber("123").smsVarslingstekst("foo").build(), builder);

        verify(builder).smsVarsel(any(SmsVarsel.class));
    }
}
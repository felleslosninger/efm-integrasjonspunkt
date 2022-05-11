package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dpi.xmlsoap.ForsendelseBuilderHandler;
import no.difi.meldingsutveksling.dpi.xmlsoap.ForsendelseHandlerFactory;
import no.difi.meldingsutveksling.dpi.xmlsoap.forsendelse.DigitalForsendelseHandler;
import no.difi.meldingsutveksling.dpi.xmlsoap.forsendelse.PrintForsendelseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

public class ForsendelseHandlerFactoryTest {

    private DigitalPostInnbyggerConfig config;
    private ForsendelseHandlerFactory factory;

    @BeforeEach
    public void setup() {
        config = new DigitalPostInnbyggerConfig();
        factory = new ForsendelseHandlerFactory(config);
    }

    @Test
    public void shouldCreateDigitalForsendelseHandler() {
        ForsendelseBuilderHandler forsendelseBuilderHandler = factory.create(MeldingsformidlerRequest.builder()
                .printProvider(false)
                .build());

        assertThat(forsendelseBuilderHandler, instanceOf(DigitalForsendelseHandler.class));
    }

    @Test
    public void shouldCreatePrintForsendelseHandler() {
        ForsendelseBuilderHandler forsendelseBuilderHandler = factory.create(MeldingsformidlerRequest.builder()
                .printProvider(true)
                .build());

        assertThat(forsendelseBuilderHandler, instanceOf(PrintForsendelseHandler.class));
    }
}
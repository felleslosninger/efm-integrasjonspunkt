package no.difi.meldingsutveksling.status;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PollingConfiguration {

    @Bean
    LinearInterpolationPolling linearInterpolationPolling(IntegrasjonspunktProperties props) {
        return new LinearInterpolationPolling(
                props.getNextmove().getStatusPollingBackoffThresholdDays(),
                props.getNextmove().getStatusPollingBackoffMaxIntervalMinutes());
    }

}

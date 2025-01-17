package no.difi.meldingsutveksling;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.UnknownHostException;

@Component
@ConditionalOnProperty(name = "difi.move.ntp.disable", havingValue = "false")
@Slf4j
public class NtpSyncChecker {

    private final IntegrasjonspunktProperties props;
    private final NTPClient client;

    NtpSyncChecker(IntegrasjonspunktProperties props) throws UnknownHostException {
        this.props = props;
        client = new NTPClient(props.getNtp().getHost());
    }

    @Scheduled(fixedRate = 1000*60*30)
    private void checkNtpOffset() {
        log.debug("Checking NTP offset");
        long offset = 0;
        try {
            offset = client.getOffset();
        } catch (IOException e) {
            log.error("Error connecting to NTP host %s".formatted(props.getNtp().getHost()), e);
        }

        String errorMsg = "Offset from NTP host %s is %sms. An offset greater than 9s might lead to problems with OIDC. Consider readjusting the system clock.".formatted(props.getNtp().getHost(), offset);
        if (Math.abs(offset) > 9000) {
            log.error(errorMsg);
        } else if (Math.abs(offset) > 5000) {
            log.warn(errorMsg);
        }
    }
}

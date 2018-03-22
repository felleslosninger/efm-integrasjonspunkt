package no.difi.meldingsutveksling;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.UnknownHostException;

@Component
@ConditionalOnProperty(name = "difi.move.disableNtpCheck", havingValue = "false")
@Slf4j
public class NtpSyncChecker {

    IntegrasjonspunktProperties props;
    NTPClient client;

    @Autowired
    NtpSyncChecker(IntegrasjonspunktProperties props) throws UnknownHostException {
        this.props = props;
        client = new NTPClient(props.getNtpHost());
    }

    @Scheduled(fixedRate = 1000*60*30)
    private void checkNtpOffset() {
        log.debug("Checking NTP offset");
        long offset = 0;
        try {
            offset = client.getOffset();
        } catch (IOException e) {
            log.error(String.format("Error connecting to NTP host %s", props.getNtpHost()), e);
        }

        String errorMsg = String.format("Offset from NTP host %s is %sms. An offset greater than 9s might lead to problems with OIDC. Consider readjusting the system clock.", props.getNtpHost(), offset);
        if (Math.abs(offset) > 9000) {
            log.error(errorMsg);
        } else if (Math.abs(offset) > 5000) {
            log.warn(errorMsg);
        }
    }
}

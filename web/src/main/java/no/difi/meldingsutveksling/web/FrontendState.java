package no.difi.meldingsutveksling.web;

import org.ocpsoft.prettytime.PrettyTime;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class FrontendState {

    private LocalDateTime started = LocalDateTime.now();
    private PrettyTime timeFormatter = new PrettyTime(Locale.of("no"));

    public String getDurationSinceStartAsText() {
        return "Integrasjonspunktet ble startet for " + timeFormatter.format(started) + ".";
    }

}

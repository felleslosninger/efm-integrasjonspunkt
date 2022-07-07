package no.difi.meldingsutveksling.ks.svarinn;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.move.common.io.pipe.Plumber;
import no.difi.move.common.io.pipe.Reject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Component
@ConditionalOnProperty(name = "difi.move.fiks.inn.enable", havingValue = "true")
@Slf4j
public class SvarInnClient {

    private final Plumber plumber;
    @Getter
    private final String rootUri;
    @Getter
    private final RestTemplate restTemplate;

    public SvarInnClient(Plumber plumber, IntegrasjonspunktProperties props, RestTemplateBuilder restTemplateBuilder) {
        this.plumber = plumber;
        this.rootUri = props.getFiks().getInn().getBaseUrl();
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(props.getFiks().getInn().getConnectTimeout()))
                .setReadTimeout(Duration.ofMillis(props.getFiks().getInn().getReadTimeout()))
                .errorHandler(new DefaultResponseErrorHandler())
                .rootUri(props.getFiks().getInn().getBaseUrl())
                .basicAuthentication(props.getFiks().getInn().getUsername(), props.getFiks().getInn().getPassword())
                .build();
    }

    List<Forsendelse> checkForNewMessages() {
        return Arrays.asList(restTemplate.getForObject("/mottaker/hentNyeForsendelser", Forsendelse[].class));
    }

    InputStreamResource downloadZipFile(Forsendelse forsendelse, Reject reject) {
        return new InputStreamResource(plumber.pipe("downloading zip file", inlet ->
                restTemplate.execute(forsendelse.getDownloadUrl(), HttpMethod.GET, null, response -> {
                    InputStream body = new AutoCloseInputStream(response.getBody());
                    int bytes = IOUtils.copy(body, inlet);
                    log.info("File for forsendelse {} was downloaded ({} bytes)", forsendelse.getId(), bytes);
                    return null;
                }), reject
        ).outlet());
    }

    void confirmMessage(String forsendelseId) {
        restTemplate.postForLocation("/kvitterMottak/forsendelse/{forsendelseId}", null, forsendelseId);
    }

    void setErrorStateForMessage(String forsendelseId, String errorMsg) {
        ErrorResponse errorResponse = new ErrorResponse(errorMsg, true);
        restTemplate.postForLocation("/mottakFeilet/forsendelse/{forsendelseId}", errorResponse, forsendelseId);
    }
}

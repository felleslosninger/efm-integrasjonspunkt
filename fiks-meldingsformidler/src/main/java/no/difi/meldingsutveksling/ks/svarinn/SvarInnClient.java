package no.difi.meldingsutveksling.ks.svarinn;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.pipes.Pipe;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@Slf4j
public class SvarInnClient {

    @Getter
    private final RestTemplate restTemplate;

    public SvarInnClient(IntegrasjonspunktProperties props, RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(props.getFiks().getInn().getConnectTimeout())
                .setReadTimeout(props.getFiks().getInn().getReadTimeout())
                .errorHandler(new DefaultResponseErrorHandler())
                .rootUri(props.getFiks().getInn().getBaseUrl())
                .basicAuthorization(props.getFiks().getInn().getUsername(), props.getFiks().getInn().getPassword())
                .build();
    }

    @PostConstruct
    public void checkTheConnection() {
        try {
            ResponseEntity<String> entity = restTemplate.getForEntity("/mottaker/hentNyeForsendelser", String.class);
            if (!entity.getStatusCode().is2xxSuccessful()) {
                throw new NextMoveRuntimeException("Couldn't check for new messages from SvarInn. Response was: " + entity.toString());
            }
        } catch (Exception e) {
            throw new NextMoveRuntimeException("Couldn't check for new messages from SvarInn.", e);
        }
    }

    List<Forsendelse> checkForNewMessages() {
        return Arrays.asList(restTemplate.getForObject("/mottaker/hentNyeForsendelser", Forsendelse[].class));
    }

    InputStream downloadZipFile(Forsendelse forsendelse) {
        return Pipe.of("downloading zip file", inlet ->
                restTemplate.execute(forsendelse.getDownloadUrl(), HttpMethod.GET, null, response -> {
                    int bytes = IOUtils.copy(response.getBody(), inlet);
                    log.info("File for forsendelse {} was downloaded ({} bytes)", forsendelse.getId(), bytes);
                    return null;
                })
        ).outlet();
    }

    void confirmMessage(String forsendelseId) {
        restTemplate.postForLocation("/kvitterMottak/forsendelse/{forsendelseId}", null, forsendelseId);
    }
}

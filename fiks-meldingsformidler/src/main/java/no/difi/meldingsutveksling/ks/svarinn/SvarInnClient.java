package no.difi.meldingsutveksling.ks.svarinn;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.pipes.Pipe;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static no.difi.meldingsutveksling.pipes.PipeOperations.copy;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@Slf4j
public class SvarInnClient {

    @Getter
    private final RestTemplate restTemplate;

    public SvarInnClient(IntegrasjonspunktProperties props) {
        this.restTemplate = new RestTemplateBuilder()
                .errorHandler(new DefaultResponseErrorHandler())
                .rootUri(props.getFiks().getInn().getBaseUrl())
                .basicAuthorization(props.getFiks().getInn().getUsername(), props.getFiks().getInn().getPassword())
                .build();
    }

    List<Forsendelse> checkForNewMessages() {
        return Arrays.asList(restTemplate.getForObject("/mottaker/hentNyeForsendelser", Forsendelse[].class));
    }

    InputStream downloadZipFile(Forsendelse forsendelse) {
        Pipe pipe = new Pipe();

        restTemplate.execute(forsendelse.getDownloadUrl(), HttpMethod.GET, null, response -> {
            pipe.consume("downloading zip file", copy(response.getBody()));
            return null;
        });

        return pipe.outlet();
    }

    void confirmMessage(String forsendelseId) {
        restTemplate.postForLocation("/kvitterMottak/forsendelse/{forsendelseId}", null, forsendelseId);
    }
}

package no.difi.meldingsutveksling.ks.svarinn;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.pipes.Pipe;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@Slf4j
public class SvarInnClient {

    @Getter
    private final RestTemplate restTemplate;

    public SvarInnClient(IntegrasjonspunktProperties props) {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        builder = builder.rootUri(props.getFiks().getInn().getBaseUrl());
        builder = builder.basicAuthorization(props.getFiks().getInn().getUsername(), props.getFiks().getInn().getPassword());
        this.restTemplate = builder.build();
    }

    List<Forsendelse> checkForNewMessages() {
        return Arrays.asList(restTemplate.getForObject("/mottaker/hentNyeForsendelser", Forsendelse[].class));
    }

    @SneakyThrows
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

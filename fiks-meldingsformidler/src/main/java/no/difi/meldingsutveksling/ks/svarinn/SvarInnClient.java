package no.difi.meldingsutveksling.ks.svarinn;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.pipes.Pipe;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class SvarInnClient {

    @Getter
    private final RestTemplate restTemplate;

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

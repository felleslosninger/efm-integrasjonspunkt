package no.difi.meldingsutveksling.ks.svarinn;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
        final PipedOutputStream source = new PipedOutputStream();
        PipedInputStream sink = new PipedInputStream(source);

        CompletableFuture.runAsync(() ->
                restTemplate.execute(forsendelse.getDownloadUrl(), HttpMethod.GET, null, response -> {
                    int bytes = IOUtils.copy(response.getBody(), source);
                    try {
                        source.flush();
                        source.close();
                    } catch (IOException e) {
                        throw new SvarInnForsendelseException("Couldn't close PipedOutputStream", e);
                    }
                    log.info("File for forsendelse {} was downloaded ({} bytes)", forsendelse.getId(), bytes);
                    return null;
                })
        ).exceptionally(ex -> {
            try {
                source.flush();
                source.close();
                sink.close();
            } catch (IOException e) {
                throw new SvarInnForsendelseException("Couldn't close PipedOutputStream", e);
            }
            return null;
        });

        return sink;
    }

    void confirmMessage(String forsendelseId) {
        restTemplate.postForLocation("/kvitterMottak/forsendelse/{forsendelseId}", null, forsendelseId);
    }
}

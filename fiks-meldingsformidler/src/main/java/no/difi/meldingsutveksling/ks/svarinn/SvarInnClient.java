package no.difi.meldingsutveksling.ks.svarinn;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class SvarInnClient {
    public static final MediaType APPLICATION_ZIP = MediaType.parseMediaType("application/zip;charset=UTF-8");

    @Getter
    private final RestTemplate restTemplate;

    public List<Forsendelse> checkForNewMessages() {
        return Arrays.asList(restTemplate.getForObject("/mottaker/hentNyeForsendelser", Forsendelse[].class));
    }

    public SvarInnFile downloadFile(String url) {
        final ResponseEntity<byte[]> forEntity = restTemplate.getForEntity(url, byte[].class);
        return new SvarInnFile("dokumenter.zip", forEntity.getHeaders().getContentType(), forEntity.getBody());
    }

    public void confirmMessage(String forsendelseId) {
        restTemplate.postForLocation("/kvitterMottak/forsendelse/{forsendelseId}", null, forsendelseId);
    }
}

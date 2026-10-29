package no.difi.meldingsutveksling.ks.svarinn;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.FiksConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.move.common.io.pipe.Plumber;
import no.difi.move.common.io.pipe.Reject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@ConditionalOnProperty(name = "difi.move.fiks.inn.enable", havingValue = "true")
@Slf4j
public class SvarInnClient {

    private final Plumber plumber;
    private final IntegrasjonspunktProperties props;

    @Getter private final String rootUri;
    @Getter private final RestTemplate restTemplate;

    public SvarInnClient(Plumber plumber, IntegrasjonspunktProperties props, RestTemplateBuilder restTemplateBuilder) {
        this.plumber = plumber;
        this.props = props;
        this.rootUri = props.getFiks().getInn().getBaseUrl();
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofMillis(props.getFiks().getInn().getConnectTimeout()))
                .readTimeout(Duration.ofMillis(props.getFiks().getInn().getReadTimeout()))
                .errorHandler(new DefaultResponseErrorHandler())
                .rootUri(props.getFiks().getInn().getBaseUrl())
                .build();
    }

    private HttpHeaders authHeadersForOrgnr(String orgnr) {
        if (props.getFiks().getInn().getPaaVegneAv().containsKey(orgnr)) {
            FiksConfig.FiksCredentials fiksAuth = props.getFiks().getInn().getPaaVegneAv().get(orgnr);
            return createHeaders(fiksAuth.getUsername(), fiksAuth.getPassword());
        }

        return createHeaders(props.getFiks().getInn().getUsername(),
                props.getFiks().getInn().getPassword());
    }

    private HttpHeaders createHeaders(String username, String password) {
        return new HttpHeaders() {{
            String auth = username + ":" + password;
            byte[] encode = Base64.getEncoder().encode(auth.getBytes(UTF_8));
            String authHeader = "Basic " + new String(encode);
            set("Authorization", authHeader);
        }};
    }

    List<Forsendelse> checkForNewMessages(String orgnr) {
        Forsendelse[] body = restTemplate.exchange("/mottaker/hentNyeForsendelser",
                HttpMethod.GET,
                new HttpEntity<>(authHeadersForOrgnr(orgnr)),
                Forsendelse[].class).getBody();
        return Arrays.asList(Objects.requireNonNull(body));
    }

    InputStreamResource downloadZipFile(Forsendelse forsendelse, Reject reject) {
        URI downloadUri = toDownloadUri(forsendelse);
        return new InputStreamResource(plumber.pipe("downloading zip file", inlet ->
                restTemplate.execute(downloadUri,
                        HttpMethod.GET,
                        request -> {
                            if (props.getFiks().getInn().getPaaVegneAv().containsKey(forsendelse.getMottaker().getOrgnr())) {
                                FiksConfig.FiksCredentials fiksCredentials = props.getFiks().getInn().getPaaVegneAv().get(forsendelse.getMottaker().getOrgnr());
                                request.getHeaders().setBasicAuth(fiksCredentials.getUsername(), fiksCredentials.getPassword(), UTF_8);
                            } else {
                                request.getHeaders().setBasicAuth(props.getFiks().getInn().getUsername(),
                                        props.getFiks().getInn().getPassword(),
                                        UTF_8);
                            }
                        }, response -> {
                            InputStream body = new AutoCloseInputStream(response.getBody());
                            int bytes = IOUtils.copy(body, inlet);
                            log.info("File for forsendelse {} was downloaded ({} bytes)", forsendelse.getId(), bytes);
                            return null;
                        }), reject
        ).outlet());
    }

    /**
     * downloadUrl comes from the SvarInn response and is used with our Fiks credentials,
     * so it must point to the configured SvarInn base URL (relative URLs are resolved against it).
     */
    URI toDownloadUri(Forsendelse forsendelse) {
        String downloadUrl = forsendelse.getDownloadUrl();
        if (downloadUrl == null) {
            throw new SvarInnForsendelseException("Forsendelse %s has no downloadUrl".formatted(forsendelse.getId()));
        }

        String root = rootUri.endsWith("/") ? rootUri.substring(0, rootUri.length() - 1) : rootUri;
        URI base = URI.create(root);
        URI uri;
        try {
            uri = new URI(downloadUrl);
            if (!uri.isAbsolute() && downloadUrl.startsWith("/") && !downloadUrl.startsWith("//")) {
                uri = new URI(root + downloadUrl);
            }
        } catch (URISyntaxException e) {
            throw new SvarInnForsendelseException("Forsendelse %s has invalid downloadUrl: %s".formatted(forsendelse.getId(), downloadUrl));
        }
        uri = uri.normalize();

        if (uri.isAbsolute()
                && base.getScheme().equalsIgnoreCase(uri.getScheme())
                && base.getHost().equals(uri.getHost())
                && base.getPort() == uri.getPort()
                && uri.getRawUserInfo() == null
                && uri.getRawPath() != null
                && uri.getRawPath().startsWith(base.getRawPath() + "/")) {
            return uri;
        }

        throw new SvarInnForsendelseException("Forsendelse %s has downloadUrl outside of %s: %s".formatted(forsendelse.getId(), rootUri, downloadUrl));
    }

    void confirmMessage(Forsendelse forsendelse) {
        restTemplate.exchange("/kvitterMottak/forsendelse/{forsendelseId}",
                HttpMethod.POST,
                new HttpEntity<>(authHeadersForOrgnr(forsendelse.getMottaker().getOrgnr())),
                Void.class,
                forsendelse.getId());
    }

    void setErrorStateForMessage(Forsendelse forsendelse, String errorMsg) {
        ErrorResponse errorResponse = new ErrorResponse(errorMsg, true);
        restTemplate.exchange("/mottakFeilet/forsendelse/{forsendelseId}",
                HttpMethod.POST,
                new HttpEntity<>(errorResponse, authHeadersForOrgnr(forsendelse.getMottaker().getOrgnr())),
                Void.class,
                forsendelse.getId());
    }

}

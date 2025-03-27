package no.difi.meldingsutveksling.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

@Component
public class MetricsRestClientInterceptor implements ClientHttpRequestInterceptor {

    private final MeterRegistry meterRegistry;

    public MetricsRestClientInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // FIXME : Verify that timings are still included : https://digdir.atlassian.net/browse/MOVE-2438
    // import org.springframework.boot.actuate.metrics.web.client.MetricsRestTemplateCustomizer;
    // import org.springframework.boot.actuate.metrics.web.client.ObservationRestTemplateCustomizer;

    @NotNull
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        long startTime = System.nanoTime();
        String method = request.getMethod().name();

        URI uri = request.getURI();
        String clientName = uri.getHost();
        String sanitizedUri = uri.getPath(); // just path (no query params)

        try {

            ClientHttpResponse response = execution.execute(request, body);

            long duration = System.nanoTime() - startTime;
            String status = String.valueOf(response.getStatusCode().value());
            String outcome = getOutcome(status);

            /*
            provide the same metrics tags as the DefaultRestTemplateExchangeTagsProvider
            method, uri, clientName, status (code), outcome (string), exception (classname, if any)
            */

            Tags commonTags = Tags.of(
                "method", method,
                "uri", sanitizedUri,
                "clientName", clientName,
                "status", status,
                "outcome", outcome,
                "exception", "None"
            );

            meterRegistry.counter("http.client.requests.total", commonTags).increment();
            meterRegistry.timer("http.client.requests.duration", commonTags).record(duration, TimeUnit.NANOSECONDS);

            // Custom metric: Count 4xx and 5xx errors separately
            if (status.startsWith("4") || status.startsWith("5")) {
                meterRegistry.counter("http.client.requests.errors", commonTags).increment();
            }

            return response;

        } catch (IOException ex) {

            long duration = System.nanoTime() - startTime;

            Tags errorTags = Tags.of(
                "method", method,
                "uri", sanitizedUri,
                "clientName", clientName,
                "status", "IO_ERROR",
                "outcome", "UNKNOWN",
                "exception", ex.getClass().getSimpleName()
            );

            meterRegistry.counter("http.client.requests.total", errorTags).increment();
            meterRegistry.timer("http.client.requests.duration", errorTags).record(duration, TimeUnit.NANOSECONDS);
            meterRegistry.counter("http.client.requests.errors", errorTags).increment();

            throw ex;
        }

    }

    private String getOutcome(String status) {
        if (status.startsWith("2")) return "SUCCESS";
        if (status.startsWith("4")) return "CLIENT_ERROR";
        if (status.startsWith("5")) return "SERVER_ERROR";
        return "UNKNOWN";
    }

}

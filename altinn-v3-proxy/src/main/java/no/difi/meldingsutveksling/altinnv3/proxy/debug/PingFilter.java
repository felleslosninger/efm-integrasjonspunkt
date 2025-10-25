package no.difi.meldingsutveksling.altinnv3.proxy.debug;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.stream.IntStream;

@Slf4j
@RequiredArgsConstructor
public class PingFilter implements GatewayFilter {

    private final MeterRegistry meterRegistry;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        var waitInMilliseconds = 0L;
        var wait = exchange.getRequest().getQueryParams().getFirst("wait");
        if (wait != null) {
            try {
                waitInMilliseconds = Long.parseLong(wait);
            } catch (Exception e) { /* ignore, we have a default value */ }
        }

        // wait can only be between 0 and 3 seconds
        if (waitInMilliseconds < 0L) waitInMilliseconds = 0;
        if (waitInMilliseconds > 3_000L) waitInMilliseconds = 3_000;

        var payload = "pong";
        var size = exchange.getRequest().getQueryParams().getFirst("size");
        if (size != null) {
            try {
                var length = Integer.parseInt(size);
                if ((length < 1) || (length > 1024*1024)) {
                    payload = "Size was %d, must be between 1 and 1 MiB".formatted(length);
                } else {
                    var random = new Random();
                    var randomChars = IntStream.range(0, length).map(i -> (65 + random.nextInt(26))).toArray();
                    var stringBuilder = new StringBuilder();
                    for (int i : randomChars) stringBuilder.append((char) i);
                    payload = stringBuilder.toString();
                }
            } catch (Exception e) { /* ignore, we have a default value */ }
        }


        final long durationInMilliseconds = waitInMilliseconds;
        final String message = payload;

        return Mono.delay(Duration.ofMillis(durationInMilliseconds))
            .then(Mono.defer(() -> {

                var response = exchange.getResponse();
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

                var buffer = response.bufferFactory()
                    .wrap(("""
                        {
                            "status" : "ok",
                            "message" : "%s",
                            "timestamp" : "%s",
                            "waited" : "%d ms"
                        }
                        """.formatted(message, LocalDateTime.now(), durationInMilliseconds)).getBytes());

                meterRegistry.counter("eformidling.dpv.proxy.ping", "method", exchange.getRequest().getMethod().name()).increment();
                return response.writeWith(Mono.just(buffer));
            }
        ));

    }

}

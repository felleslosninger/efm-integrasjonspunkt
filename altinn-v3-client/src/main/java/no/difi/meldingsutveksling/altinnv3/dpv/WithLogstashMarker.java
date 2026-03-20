package no.difi.meldingsutveksling.altinnv3.dpv;

import lombok.RequiredArgsConstructor;
import net.logstash.logback.marker.LogstashMarker;

import java.util.function.Supplier;


@RequiredArgsConstructor(staticName = "withLogstashMarker")
public class WithLogstashMarker {

    private final LogstashMarker logstashMarker;

    public <T> T execute(Supplier<T> action) {
        try {
            LogstashMarkerHolder.set(logstashMarker);
            return action.get();
        } finally {
            LogstashMarkerHolder.remove();
        }
    }
}

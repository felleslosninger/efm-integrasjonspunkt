package no.difi.meldingsutveksling.altinnv3.dpv;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.LogstashMarker;

@UtilityClass
@Slf4j
public class LogstashMarkerHolder {

    private static ThreadLocal<LogstashMarker> holder = new ThreadLocal<>();

    public static LogstashMarker get() {
        return holder.get();
    }

    public static void set(LogstashMarker logstashMarker) {
        if (holder.get() != null) {
            log.warn("Logstash marker already present!");
        }

        holder.set(logstashMarker);
    }

    public static void remove() {
        holder.remove();
    }
}

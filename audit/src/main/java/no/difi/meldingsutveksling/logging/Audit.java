package no.difi.meldingsutveksling.logging;

import net.logstash.logback.marker.LogstashMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

/**
 * Class used to log audit messages
 */
public class Audit {
    private Audit() {
    }

    public static final Logger logger = LoggerFactory.getLogger("AUDIT");

    public static void info(String text, LogstashMarker marker) {
        logger.info(marker, text);
    }

    public static void error(String text, Marker marker) {
        logger.error(marker, text);
    }

    public static void error(String text, LogstashMarker marker, Object... args) {
        logger.error(marker, text);
    }

    public static void info(String text) {
        logger.info(text);
    }

    public static void error(String text) {
        logger.error(text);
    }

    public static void warn(String text, LogstashMarker marker) {
        logger.warn(marker, text);
    }

}

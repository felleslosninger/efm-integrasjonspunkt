package no.difi.meldingsutveksling.logging;

import net.logstash.logback.marker.LogstashMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to log audit messages
 */
public class Audit {
    public static Logger logger = LoggerFactory.getLogger("AUDIT");

    public static void info(String text, LogstashMarker marker) {
        logger.info(marker, text);
    }

    public static void error(String text, LogstashMarker marker) {
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
}

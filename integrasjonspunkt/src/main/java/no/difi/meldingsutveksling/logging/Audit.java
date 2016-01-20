package no.difi.meldingsutveksling.logging;

import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.FileReference;
import no.difi.meldingsutveksling.domain.sbdh.Document;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
import no.difi.meldingsutveksling.noarkexchange.StandardBusinessDocumentWrapper;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to log audit messages
 */
public class Audit {
    public static Logger logger = LoggerFactory.getLogger("AUDIT");

    public static void info(String text, StandardBusinessDocument standardBusinessDocument) {
        info(text, new StandardBusinessDocumentWrapper(standardBusinessDocument));
    }

    public static void info(String text, PutMessageRequestWrapper putMessageRequestWrapper) {
        logger.info(MessageMarkerFactory.markerFrom(putMessageRequestWrapper), text);
    }

    public static void error(String text, PutMessageRequestWrapper message) {
        logger.error(MessageMarkerFactory.markerFrom(message), text);
    }

    public static void error(String text, StandardBusinessDocument standardBusinessDocument) {
        final LogstashMarker marker = MessageMarkerFactory.markerFrom(new StandardBusinessDocumentWrapper(standardBusinessDocument));
        logger.error(marker, text);
    }

    public static void info(String text) {
        logger.info(text);
    }

    public static void info(FileReference reference, String text) {
        LogstashMarker fileReferenceMarker = MessageMarkerFactory.markerFrom(reference);
        logger.info(fileReferenceMarker, text);
    }

    public static void info(String text, StandardBusinessDocumentWrapper standardBusinessDocument) {
        final LogstashMarker marker = MessageMarkerFactory.markerFrom(standardBusinessDocument);
        logger.info(marker, text);

    }

    public static void error(String text) {

    }
}

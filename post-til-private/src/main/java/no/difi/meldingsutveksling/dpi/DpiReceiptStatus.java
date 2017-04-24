package no.difi.meldingsutveksling.dpi;

import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.sdp.client2.domain.kvittering.AapningsKvittering;
import no.difi.sdp.client2.domain.kvittering.Feil;
import no.difi.sdp.client2.domain.kvittering.ForretningsKvittering;
import no.difi.sdp.client2.domain.kvittering.LeveringsKvittering;
import no.difi.sdp.client2.domain.kvittering.MottaksKvittering;
import no.difi.sdp.client2.domain.kvittering.ReturpostKvittering;
import no.difi.sdp.client2.domain.kvittering.VarslingFeiletKvittering;

import java.util.HashMap;
import java.util.function.BiConsumer;

public enum DpiReceiptStatus implements ReceiptStatus {
    DELIVERED("Kvittering på at digital post er tilgjengeliggjort eller at en fysisk post er postlagt", Audit::info),
    READ("Kvittering fra Innbygger for at digital post er åpnet", Audit::info),
    NOTIFICATION_FAILED("Kvittering for at en spesifisert varsling ikke har blitt sendt", Audit::error),
    READY_FOR_PRINT("Kvittering fra utskrift og forsendelsestjenesten om at melding er mottatt og lagt til print", Audit::info),
    POST_RETURNED("Kvittering fra utskrift og forsendelsestjenesten om at posten ikke har blitt levert til Mottaker.", Audit::warn),
    FEIL("Generell melding om at det har skjedd en feil", Audit::error),
    UNKNOWN("Kvittering ukjent for integrasjonspunktet", Audit::warn);


    private static final HashMap<Class, DpiReceiptStatus> mapper;
    static {
        mapper = new HashMap<>();
        mapper.put(LeveringsKvittering.class, DELIVERED);
        mapper.put(AapningsKvittering.class, READ);
        mapper.put(VarslingFeiletKvittering.class, NOTIFICATION_FAILED);
        mapper.put(MottaksKvittering.class, READY_FOR_PRINT);
        mapper.put(ReturpostKvittering.class, POST_RETURNED);
        mapper.put(Feil.class, FEIL);
    }

    final String status;
    private final BiConsumer<String, LogstashMarker> loggerMethod;

    DpiReceiptStatus(String description, BiConsumer<String, LogstashMarker> loggerMethod) {
        this.status = description;
        this.loggerMethod = loggerMethod;
    }

    @Override
    public String getStatus() {
        return this.status;
    }

    public void invokeLoggerMethod(LogstashMarker logstashMarkers) {
        loggerMethod.accept(status, logstashMarkers);
    }

    public static DpiReceiptStatus from(ForretningsKvittering forretningsKvittering) {
        return mapper.getOrDefault(forretningsKvittering.getClass(), UNKNOWN);
    }

}

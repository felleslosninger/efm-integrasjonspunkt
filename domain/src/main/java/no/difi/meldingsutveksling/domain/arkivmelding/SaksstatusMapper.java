package no.difi.meldingsutveksling.domain.arkivmelding;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.metadatakatalog.Saksstatus;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

@Slf4j
@UtilityClass
public class SaksstatusMapper {
    private static final Map<String, Saksstatus> string2Status = new HashMap<>();
    private static final Map<Saksstatus, String> status2string = new HashMap<>();

    static {
        string2Status.put("A", Saksstatus.AVSLUTTET);
        string2Status.put("B", Saksstatus.UNDER_BEHANDLING);
        string2Status.put("U", Saksstatus.UTGÅR);
        string2Status.put("R", Saksstatus.UNDER_BEHANDLING); // Reservert sak. (Midlertidig = 1, Lukket = 0)
        string2Status.put("X", Saksstatus.UNDER_BEHANDLING); //  Saken er ikke gjenstand for oppfølging, tilsvarer blankt felt Sak avsluttet i Noark-3. (Midlertidig = 0, Lukket = 0)
        string2Status.put("KU", Saksstatus.AVSLUTTET); // Kopiert utdrag. Skal kun benyttes ved eksport av et utdrag av en sak og da kun i den eksporterte versjonen av saken. Ved import av  en sak med status KU skal den håndteres på linje med status A, dvs saken skal ikke kunne endres.

        status2string.put(Saksstatus.AVSLUTTET, "A");
        status2string.put(Saksstatus.UNDER_BEHANDLING, "B");
        status2string.put(Saksstatus.UTGÅR, "U");
    }

    public static String getNoarkType(Saksstatus status) {
        if (status == null || !string2Status.containsValue(status)) {
            log.warn("Saksstatus \"{}\" not registered in map, defaulting to \"{}\"", status != null ? status.value() : "<none>", Saksstatus.UNDER_BEHANDLING.value());
            return status2string.get(Saksstatus.UNDER_BEHANDLING);
        }

        return status2string.get(status);
    }

    static Saksstatus getArkivmeldingType(String status) {
        if (isNullOrEmpty(status) || !string2Status.containsKey(status)) {
            log.warn("Saksstatus \"{}\" not registered in map, defaulting to \"{}\"", status, status2string.get(Saksstatus.UNDER_BEHANDLING));
            return Saksstatus.UNDER_BEHANDLING;
        }

        return string2Status.get(status);
    }
}

package no.difi.meldingsutveksling.domain.arkivmelding;

import com.google.common.collect.HashBiMap;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.metadatakatalog.Journalstatus;

@Slf4j
public class JournalstatusMapper {
    private static final HashBiMap<String, Journalstatus> mapper = HashBiMap.create();

    private JournalstatusMapper() {
    }

    static {
        mapper.put("R", Journalstatus.FERDIGSTILT_FRA_SAKSBEHANDLER);
        mapper.put("E", Journalstatus.EKSPEDERT);
        mapper.put("A", Journalstatus.ARKIVERT);
        mapper.put("F", Journalstatus.GODKJENT_AV_LEDER);
        mapper.put("J", Journalstatus.JOURNALFØRT);
        mapper.put("U", Journalstatus.UTGÅR);
    }

    public static String getNoarkType(Journalstatus status) {
        if (!mapper.containsValue(status)) {
            log.error("Journalposttype \"{}\" not registered in map, defaulting to \"{}\"", status.value(), Journalstatus.FERDIGSTILT_FRA_SAKSBEHANDLER.value());
            return mapper.inverse().get(Journalstatus.EKSPEDERT);
        }

        return mapper.inverse().get(status);
    }

    public static Journalstatus getArkivmeldingType(String jpType) {
        if (!mapper.containsKey(jpType)) {
            log.error("Journalposttype \"{}\" not registered in map, defaulting to \"{}\"", jpType, mapper.inverse().get(Journalstatus.FERDIGSTILT_FRA_SAKSBEHANDLER));
            return Journalstatus.EKSPEDERT;
        }

        return mapper.get(jpType);
    }
}

package no.difi.meldingsutveksling.domain.arkivmelding;

import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.metadatakatalog.Journalstatus;
import no.difi.meldingsutveksling.HashBiMap;

import static com.google.common.base.Strings.isNullOrEmpty;

@Slf4j
public class JournalstatusMapper {
    private static final HashBiMap<String, Journalstatus> mapper = new HashBiMap<>();

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
        if (status == null || !mapper.containsValue(status)) {
            log.warn("Journalposttype \"{}\" not registered in map, defaulting to \"{}\"", status != null ? status.value() : "<none>", Journalstatus.FERDIGSTILT_FRA_SAKSBEHANDLER.value());
            return mapper.inverse().get(Journalstatus.FERDIGSTILT_FRA_SAKSBEHANDLER);
        }

        return mapper.inverse().get(status);
    }

    public static Journalstatus getArkivmeldingType(String jpType) {
        if (isNullOrEmpty(jpType) || !mapper.containsKey(jpType)) {
            log.warn("Journalposttype \"{}\" not registered in map, defaulting to \"{}\"", jpType, mapper.inverse().get(Journalstatus.FERDIGSTILT_FRA_SAKSBEHANDLER));
            return Journalstatus.FERDIGSTILT_FRA_SAKSBEHANDLER;
        }

        return mapper.get(jpType);
    }

}

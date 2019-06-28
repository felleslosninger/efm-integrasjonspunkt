package no.difi.meldingsutveksling.domain.arkivmelding;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.metadatakatalog.Saksstatus;
import no.difi.meldingsutveksling.HashBiMap;

@Slf4j
@UtilityClass
public class SaksstatusMapper {
    private static final HashBiMap<String, Saksstatus> mapper = new HashBiMap<>();

    static {
        mapper.put("A", Saksstatus.AVSLUTTET);
        mapper.put("B", Saksstatus.UNDER_BEHANDLING);
        mapper.put("U", Saksstatus.UTGÅR);
    }

    public static String getNoarkType(Saksstatus status) {
        if (!mapper.containsValue(status)) {
            log.error("Saksstatus \"{}\" not registered in map, defaulting to \"{}\"", status.value(), Saksstatus.UNDER_BEHANDLING.value());
            return mapper.inverse().get(Saksstatus.UNDER_BEHANDLING);
        }

        return mapper.inverse().get(status);
    }

    static Saksstatus getArkivmeldingType(String status) {
        if (!mapper.containsKey(status)) {
            log.error("Saksstatus \"{}\" not registered in map, defaulting to \"{}\"", status, mapper.inverse().get(Saksstatus.UNDER_BEHANDLING));
            return Saksstatus.UNDER_BEHANDLING;
        }

        return mapper.get(status);
    }
}

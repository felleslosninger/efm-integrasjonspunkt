package no.difi.meldingsutveksling.domain.arkivmelding;

import com.google.common.collect.HashBiMap;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.metadatakatalog.Saksstatus;

@Slf4j
public class SaksstatusMapper {
    private static final HashBiMap<String, Saksstatus> mapper = HashBiMap.create();

    private SaksstatusMapper() {
    }

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

    public static Saksstatus getArkivmeldingType(String status) {
        if (!mapper.containsKey(status)) {
            log.error("Saksstatus \"{}\" not registered in map, defaulting to \"{}\"", status, mapper.inverse().get(Saksstatus.UNDER_BEHANDLING));
            return Saksstatus.UNDER_BEHANDLING;
        }

        return mapper.get(status);
    }
}

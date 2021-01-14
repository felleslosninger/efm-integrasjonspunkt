package no.difi.meldingsutveksling.domain.arkivmelding;

import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.metadatakatalog.TilknyttetRegistreringSom;
import no.difi.meldingsutveksling.HashBiMap;

import static com.google.common.base.Strings.isNullOrEmpty;

@Slf4j
public class TilknyttetRegistreringSomMapper {

    private static final HashBiMap<String, TilknyttetRegistreringSom> mapper = new HashBiMap<>();

    private TilknyttetRegistreringSomMapper() {
    }

    static {
        mapper.put("H", TilknyttetRegistreringSom.HOVEDDOKUMENT);
        mapper.put("V", TilknyttetRegistreringSom.VEDLEGG);
    }

    public static String getNoarkType(TilknyttetRegistreringSom trs) {
        if (trs == null || !mapper.containsValue(trs)) {
            log.warn("TilknyttetRegistreringSom \"{}\" not registered in map, defaulting to \"{}\"", trs != null ? trs.value() : "<none>", TilknyttetRegistreringSom.HOVEDDOKUMENT.value());
            return mapper.inverse().get(TilknyttetRegistreringSom.HOVEDDOKUMENT);
        }
        return mapper.inverse().get(trs);
    }

    public static TilknyttetRegistreringSom getArkivmeldingType(String dlType) {
        if (isNullOrEmpty(dlType) || !mapper.containsKey(dlType)) {
            log.warn("DlType \"{}\" not registered in map, defaulting to \"{}\"", dlType, TilknyttetRegistreringSom.HOVEDDOKUMENT.value());
            return TilknyttetRegistreringSom.HOVEDDOKUMENT;
        }
        return mapper.get(dlType);
    }
}
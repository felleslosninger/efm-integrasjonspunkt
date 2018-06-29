package no.difi.meldingsutveksling.arkivmelding;

import com.google.common.collect.HashBiMap;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.metadatakatalog.TilknyttetRegistreringSom;

@Slf4j
public class TilknyttetRegistreringSomMapper {

    private static final HashBiMap<String, TilknyttetRegistreringSom> mapper = HashBiMap.create();

    private TilknyttetRegistreringSomMapper() {
    }

    static {
        mapper.put("H", TilknyttetRegistreringSom.HOVEDDOKUMENT);
        mapper.put("V", TilknyttetRegistreringSom.VEDLEGG);
    }

    public static String getNoarkType(TilknyttetRegistreringSom trs) {
        if (!mapper.containsValue(trs)) {
            log.error("TilknyttetRegistreringSom \"{}\" not registered in map, defaulting to \"{}\"", trs.value(), TilknyttetRegistreringSom.HOVEDDOKUMENT.value());
            return mapper.inverse().get(TilknyttetRegistreringSom.HOVEDDOKUMENT);
        }
        return mapper.inverse().get(trs);
    }

    public static TilknyttetRegistreringSom getArkivmeldingType(String dlType) {
        if (!mapper.containsKey(dlType)) {
            log.error("DlType \"{}\" not registered in map, defaulting to \"{}\"", dlType, TilknyttetRegistreringSom.HOVEDDOKUMENT.value());
            return TilknyttetRegistreringSom.HOVEDDOKUMENT;
        }
        return mapper.get(dlType);
    }
}
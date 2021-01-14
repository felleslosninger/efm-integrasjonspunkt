package no.difi.meldingsutveksling.domain.arkivmelding;

import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.metadatakatalog.Journalposttype;
import no.difi.meldingsutveksling.HashBiMap;

import static com.google.common.base.Strings.isNullOrEmpty;

@Slf4j
public class JournalposttypeMapper {

    private static final HashBiMap<String, Journalposttype> mapper = new HashBiMap<>();

    private JournalposttypeMapper() {
    }

    static {
        mapper.put("I", Journalposttype.INNGÅENDE_DOKUMENT);
        mapper.put("U", Journalposttype.UTGÅENDE_DOKUMENT);
        mapper.put("S", Journalposttype.SAKSFRAMLEGG);
        mapper.put("N", Journalposttype.ORGANINTERNT_DOKUMENT_FOR_OPPFØLGING);
        mapper.put("X", Journalposttype.ORGANINTERNT_DOKUMENT_UTEN_OPPFØLGING);
    }

    public static String getNoarkType(Journalposttype jpType) {
        if (jpType == null || !mapper.containsValue(jpType)) {
            log.warn("Journalposttype \"{}\" not registered in map, defaulting to \"{}\"", jpType != null ? jpType.value() : "<none>", Journalposttype.INNGÅENDE_DOKUMENT.value());
            return mapper.inverse().get(Journalposttype.INNGÅENDE_DOKUMENT);
        }

        return mapper.inverse().get(jpType);
    }

    public static Journalposttype getArkivmeldingType(String jpType) {
        if (isNullOrEmpty(jpType) || !mapper.containsKey(jpType)) {
            log.warn("Journalposttype \"{}\" not registered in map, defaulting to \"{}\"", jpType, mapper.inverse().get(Journalposttype.INNGÅENDE_DOKUMENT));
            return Journalposttype.INNGÅENDE_DOKUMENT;
        }

        return mapper.get(jpType);
    }

}
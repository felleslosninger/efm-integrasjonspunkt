package no.difi.meldingsutveksling.domain.arkivmelding;

import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.metadatakatalog.Journalposttype;
import no.difi.meldingsutveksling.HashBiMap;

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
        if (!mapper.containsValue(jpType)) {
            log.warn("Journalposttype \"{}\" not registered in map, defaulting to \"{}\"", jpType.value(), Journalposttype.INNGÅENDE_DOKUMENT.value());
            return mapper.inverse().get(Journalposttype.INNGÅENDE_DOKUMENT);
        }

        return mapper.inverse().get(jpType);
    }

    public static Journalposttype getArkivmeldingType(String jpType) {
        if (!mapper.containsKey(jpType)) {
            log.warn("Journalposttype \"{}\" not registered in map, defaulting to \"{}\"", jpType, mapper.inverse().get(Journalposttype.INNGÅENDE_DOKUMENT));
            return Journalposttype.INNGÅENDE_DOKUMENT;
        }

        return mapper.get(jpType);
    }

    public static Journalposttype getArkivmeldingTypeFromFiksValue(String fiksType) {
        try {
            return Journalposttype.fromValue(fiksType);
        } catch (IllegalArgumentException e) {
            log.warn("Cannot map \"{}\" to Journalposttype, defaulting to \"{}\"", fiksType, Journalposttype.INNGÅENDE_DOKUMENT.value());
            return Journalposttype.INNGÅENDE_DOKUMENT;
        }
    }

}
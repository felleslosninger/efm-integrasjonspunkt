package no.difi.meldingsutveksling.ks.svarinn;

import lombok.Data;
import lombok.NonNull;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.noarkexchange.schema.core.*;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class SvarInnMessage {
    @NonNull
    Forsendelse forsendelse;
    @NonNull
    List<SvarInnFile> svarInnFiles;

    EDUCore toEduCore() {
        ObjectFactory objectFactory = new ObjectFactory();

        final MeldingType meldingType = objectFactory.createMeldingType();
        final JournpostType journpostType = objectFactory.createJournpostType();
        final List<DokumentType> collect = svarInnFiles.stream().map(sif -> {
            final DokumentType dokumentType = objectFactory.createDokumentType();
            dokumentType.setVeMimeType(sif.getMediaType().toString());
            final FilType fil = objectFactory.createFilType();
            fil.setBase64(sif.getContents());
            dokumentType.setFil(fil);
            return dokumentType;

        }).collect(Collectors.toList());

        journpostType.getDokument().addAll(collect);
        meldingType.setJournpost(journpostType);
        final EDUCore eduCore = new EDUCore();
        eduCore.setPayload(meldingType);
        return eduCore;
    }
}

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
//        journpostType.setJpDokdato(forsendelse.getMetadataForImport());
        JournpostType journpostType = createJournpostType();
        final List<DokumentType> dokumentTypes = svarInnFiles.stream().map(sif -> {
            final DokumentType dokumentType = objectFactory.createDokumentType();
            dokumentType.setVeMimeType(sif.getMediaType().toString());
            final FilType fil = objectFactory.createFilType();
            fil.setBase64(sif.getContents());
            dokumentType.setFil(fil);
            return dokumentType;

        }).collect(Collectors.toList());

        journpostType.getDokument().addAll(dokumentTypes);
        NoarksakType noarkSak = createNoarkSak();
        meldingType.setNoarksak(noarkSak);
        meldingType.setJournpost(journpostType);
        final EDUCore eduCore = new EDUCore();
        eduCore.setPayload(meldingType);
        return eduCore;
    }

    private NoarksakType createNoarkSak() {
        ObjectFactory objectFactory = new ObjectFactory();
        final NoarksakType noarksakType = objectFactory.createNoarksakType();
        final Forsendelse.MetadataForImport metadataForImport = forsendelse.getMetadataForImport();
        noarksakType.setSaSeknr(String.valueOf(metadataForImport.getSakssekvensnummer()));
        noarksakType.setSaSaar(String.valueOf(metadataForImport.getSaksaar()));
        noarksakType.setSaTittel(metadataForImport.getTittel());
        return noarksakType;
    }

    private JournpostType createJournpostType() {
        ObjectFactory objectFactory = new ObjectFactory();
        final JournpostType journpostType = objectFactory.createJournpostType();
        final Forsendelse.MetadataForImport metadataForImport = forsendelse.getMetadataForImport();
        journpostType.setJpDokdato(metadataForImport.getDokumentetsDato());
        journpostType.setJpNdoktype(metadataForImport.getJournalposttype());
        journpostType.setJpStatus(metadataForImport.getJournalstatus());
        return journpostType;
    }
}

package no.difi.meldingsutveksling.ks.svarinn;

import lombok.Data;
import lombok.NonNull;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.Receiver;
import no.difi.meldingsutveksling.core.Sender;
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
        eduCore.setSender(createSender());
        eduCore.setReceiver(createReceiver());
        return eduCore;
    }

    private Sender createSender() {
        final Sender sender = new Sender();
        sender.setIdentifier(forsendelse.getSvarSendesTil().getOrgnr());
        return sender;
    }

    private Receiver createReceiver() {
        Receiver receiver = new Receiver();
        receiver.setIdentifier(forsendelse.getMottaker().getOrgnr());
        return receiver;
    }

    private NoarksakType createNoarkSak() {
        ObjectFactory objectFactory = new ObjectFactory();
        final NoarksakType noarksakType = objectFactory.createNoarksakType();
        final Forsendelse.MetadataFraAvleverendeSystem metadata = forsendelse.getMetadataFraAvleverendeSystem();
        noarksakType.setSaSeknr(String.valueOf(metadata.getSakssekvensnummer()));
        noarksakType.setSaSaar(String.valueOf(metadata.getSaksaar()));
        noarksakType.setSaTittel(metadata.getTittel());
        return noarksakType;
    }

    private JournpostType createJournpostType() {
        ObjectFactory objectFactory = new ObjectFactory();
        final JournpostType journpostType = objectFactory.createJournpostType();
        final Forsendelse.MetadataFraAvleverendeSystem metadata = forsendelse.getMetadataFraAvleverendeSystem();
        journpostType.setJpDokdato(metadata.getDokumentetsDato());
        journpostType.setJpNdoktype(metadata.getJournalposttype());
        journpostType.setJpStatus(metadata.getJournalstatus());
        journpostType.setJpJaar(metadata.getJournalaar());
        journpostType.setJpSeknr(metadata.getJournalsekvensnummer());
        journpostType.setJpJpostnr(metadata.getJournalpostnummer());
        return journpostType;
    }
}

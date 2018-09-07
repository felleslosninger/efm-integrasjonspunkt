package no.difi.meldingsutveksling.ks.svarinn;

import lombok.Data;
import lombok.NonNull;
import no.difi.meldingsutveksling.MimeTypeExtensionMapper;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.Receiver;
import no.difi.meldingsutveksling.core.Sender;
import no.difi.meldingsutveksling.noarkexchange.receive.PayloadConverter;
import no.difi.meldingsutveksling.noarkexchange.receive.PayloadConverterImpl;
import no.difi.meldingsutveksling.noarkexchange.schema.core.*;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

@Data
public class SvarInnMessage {
    @NonNull
    Forsendelse forsendelse;
    @NonNull
    List<SvarInnFile> svarInnFiles;
    @NonNull
    IntegrasjonspunktProperties properties;

    private PayloadConverter<MeldingType> payloadConverter = new PayloadConverterImpl<>(MeldingType.class,
            "http://www.arkivverket.no/Noark4-1-WS-WD/types", "Melding");

    EDUCore toEduCore() {
        ObjectFactory objectFactory = new ObjectFactory();

        final MeldingType meldingType = objectFactory.createMeldingType();
        JournpostType journpostType = createJournpostType();
        final List<DokumentType> dokumentTypes = svarInnFiles.stream().map(sif -> {
            final DokumentType dokumentType = objectFactory.createDokumentType();

            String mimeType = sif.getMediaType().toString();
            dokumentType.setVeMimeType(mimeType);
            dokumentType.setVeDokformat(MimeTypeExtensionMapper.getExtension(mimeType));

            dokumentType.setDbTittel(sif.getFilnavn());
            dokumentType.setVeFilnavn(sif.getFilnavn());
            // Value does not exist in FIKS, must be hard coded
            dokumentType.setVeVariant("P");
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

        // this is done because of a bug in when sending messages via NoarkClient:
        // The payload util doesn't correctly handle payloads that are MeldingType.
        // And I am afraid of fixing that bug might lead to trouble in the archive system.
        eduCore.setPayload(payloadConverter.marshallToString(meldingType));
        eduCore.setId(forsendelse.getId());
        eduCore.setSender(createSender());
        eduCore.setReceiver(createReceiver());
        eduCore.setServiceIdentifier(ServiceIdentifier.DPF);
        return eduCore;
    }

    private Sender createSender() {
        if (isNullOrEmpty(forsendelse.getSvarSendesTil().getOrgnr()) && !isNullOrEmpty(properties.getFiks().getInn().getFallbackSenderOrgNr())) {
            return Sender.of(properties.getFiks().getInn().getFallbackSenderOrgNr(), forsendelse.getSvarSendesTil().getNavn(), forsendelse.getId());
        }
        return Sender.of(forsendelse.getSvarSendesTil().getOrgnr(), forsendelse.getSvarSendesTil().getNavn(), forsendelse.getId());
    }

    private Receiver createReceiver() {
        return Receiver.of(forsendelse.getMottaker().getOrgnr(), forsendelse.getMottaker().getNavn(), forsendelse.getSvarPaForsendelse());
    }

    private NoarksakType createNoarkSak() {
        ObjectFactory objectFactory = new ObjectFactory();
        final NoarksakType noarksakType = objectFactory.createNoarksakType();
        final Forsendelse.MetadataFraAvleverendeSystem metadata = forsendelse.getMetadataFraAvleverendeSystem();
        noarksakType.setSaSeknr(String.valueOf(metadata.getSakssekvensnummer()));
        noarksakType.setSaSaar(String.valueOf(metadata.getSaksaar()));
        noarksakType.setSaTittel(isNullOrEmpty(metadata.getTittel()) ? "Saken mangler tittel" : metadata.getTittel());
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
        journpostType.setJpOffinnhold(isNullOrEmpty(metadata.getTittel()) ? "Dokumentet mangler tittel" : metadata.getTittel());
        journpostType.setJpInnhold(isNullOrEmpty(metadata.getTittel()) ? "Dokumentet mangler tittel" : metadata.getTittel());
        journpostType.setJpJdato(metadata.getJournaldato());
        journpostType.getAvsmot().add(createSaksbehandlerAvsender(metadata));
        journpostType.getAvsmot().add(createAvsender());
        return journpostType;
    }

    private AvsmotType createAvsender() {
        Forsendelse.SvarSendesTil sst = forsendelse.getSvarSendesTil();
        AvsmotType avsender = new AvsmotType();
        avsender.setAmIhtype("1");
        avsender.setAmAdresse(sst.getAdresse1());
        avsender.setAmPostnr(sst.getPostnr());
        avsender.setAmPoststed(sst.getPoststed());
        avsender.setAmNavn(sst.getNavn());
        avsender.setAmUtland(sst.getLand());
        avsender.setAmOrgnr(sst.getOrgnr());
        return avsender;
    }

    private AvsmotType createSaksbehandlerAvsender(Forsendelse.MetadataFraAvleverendeSystem metadata){
        AvsmotType avsender = new AvsmotType();
        avsender.setAmIhtype("0");
        avsender.setAmNavn(metadata.getSaksBehandler());
        return avsender;
    }
}

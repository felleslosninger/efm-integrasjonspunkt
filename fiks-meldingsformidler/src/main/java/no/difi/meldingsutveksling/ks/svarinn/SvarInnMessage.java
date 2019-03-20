package no.difi.meldingsutveksling.ks.svarinn;

import lombok.Data;
import lombok.NonNull;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.arkivverket.standarder.noark5.arkivmelding.Journalpost;
import no.arkivverket.standarder.noark5.arkivmelding.Korrespondansepart;
import no.arkivverket.standarder.noark5.arkivmelding.Saksmappe;
import no.arkivverket.standarder.noark5.metadatakatalog.Korrespondanseparttype;
import no.difi.meldingsutveksling.MimeTypeExtensionMapper;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.Receiver;
import no.difi.meldingsutveksling.core.Sender;
import no.difi.meldingsutveksling.domain.arkivmelding.JournalposttypeMapper;
import no.difi.meldingsutveksling.domain.arkivmelding.JournalstatusMapper;
import no.difi.meldingsutveksling.noarkexchange.receive.PayloadConverter;
import no.difi.meldingsutveksling.noarkexchange.receive.PayloadConverterImpl;
import no.difi.meldingsutveksling.noarkexchange.schema.core.*;

import java.math.BigInteger;
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

    Arkivmelding toArkivmelding() {
        no.arkivverket.standarder.noark5.arkivmelding.ObjectFactory of = new no.arkivverket.standarder.noark5.arkivmelding.ObjectFactory();

        Journalpost journalpost = of.createJournalpost();
        journalpost.setOffentligTittel(forsendelse.getTittel());

        Korrespondansepart avsender = of.createKorrespondansepart();
        avsender.setKorrespondanseparttype(Korrespondanseparttype.AVSENDER);
        Forsendelse.SvarSendesTil sst = forsendelse.getSvarSendesTil();
        avsender.setKorrespondansepartNavn(sst.getNavn());
        avsender.getPostadresse().add(sst.getAdresse1());
        avsender.setPostnummer(sst.getPostnr());
        avsender.setPoststed(sst.getPoststed());
        avsender.setLand(sst.getLand());
        journalpost.getKorrespondansepart().add(avsender);

        Saksmappe saksmappe = of.createSaksmappe();
        Forsendelse.MetadataFraAvleverendeSystem metadata = forsendelse.getMetadataFraAvleverendeSystem();
        saksmappe.setSakssekvensnummer(BigInteger.valueOf(metadata.getSakssekvensnummer()));
        saksmappe.setSaksaar(BigInteger.valueOf(metadata.getSaksaar()));
        saksmappe.setSaksansvarlig(metadata.getSaksBehandler());

        journalpost.setJournalaar(BigInteger.valueOf(Long.valueOf(metadata.getJournalaar())));
        journalpost.setJournalsekvensnummer(BigInteger.valueOf(Long.valueOf(metadata.getJournalsekvensnummer())));
        journalpost.setJournalpostnummer(BigInteger.valueOf(Long.valueOf(metadata.getJournalpostnummer())));
        journalpost.setJournalposttype(JournalposttypeMapper.getArkivmeldingType(metadata.getJournalposttype()));
        journalpost.setJournalstatus(JournalstatusMapper.getArkivmeldingType(metadata.getJournalstatus()));
        journalpost.setJournaldato(ArkivmeldingUtil.stringAsXmlGregorianCalendar(metadata.getJournaldato()));
        journalpost.setDokumentetsDato(ArkivmeldingUtil.stringAsXmlGregorianCalendar(metadata.getDokumentetsDato()));
        journalpost.setOffentligTittel(metadata.getTittel());

        saksmappe.getBasisregistrering().add(journalpost);
        Arkivmelding arkivmelding = of.createArkivmelding();
        arkivmelding.getMappe().add(saksmappe);

        return arkivmelding;
    }

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
        noarksakType.setSaTittel(getForsendelseTittel());
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
        journpostType.setJpOffinnhold(getForsendelseTittel());
        journpostType.setJpInnhold(getForsendelseTittel());
        journpostType.setJpJdato(metadata.getJournaldato());
        journpostType.getAvsmot().add(createSaksbehandlerAvsender(metadata));
        journpostType.getAvsmot().add(createAvsender());
        return journpostType;
    }

    private String getForsendelseTittel() {
        if (!isNullOrEmpty(forsendelse.getMetadataFraAvleverendeSystem().getTittel())) {
            return forsendelse.getMetadataFraAvleverendeSystem().getTittel();
        }
        if (!isNullOrEmpty(forsendelse.getTittel())) {
            return forsendelse.getTittel();
        }
        return "Dokumentet mangler tittel";
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

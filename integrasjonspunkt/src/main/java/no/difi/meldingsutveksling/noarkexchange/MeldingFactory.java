package no.difi.meldingsutveksling.noarkexchange;

import lombok.RequiredArgsConstructor;
import no.arkivverket.standarder.noark5.arkivmelding.*;
import no.difi.meldingsutveksling.DateTimeUtil;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.arkivmelding.*;
import no.difi.meldingsutveksling.noarkexchange.schema.core.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.core.*;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Optional.ofNullable;
import static no.difi.meldingsutveksling.MimeTypeExtensionMapper.getMimetype;

@Component
@RequiredArgsConstructor
public class MeldingFactory {

    private final ArkivmeldingUtil arkivmeldingUtil;

    public MeldingType create(Arkivmelding am, Resource asic) {
        Saksmappe sm = arkivmeldingUtil.getSaksmappe(am);
        Journalpost jp = arkivmeldingUtil.getJournalpost(am);

        ObjectFactory of = new ObjectFactory();
        NoarksakType noarksakType = of.createNoarksakType();
        ofNullable(sm.getSaksaar()).map(BigInteger::toString).ifPresent(noarksakType::setSaSaar);
        ofNullable(sm.getSakssekvensnummer()).map(BigInteger::toString).ifPresent(noarksakType::setSaSeknr);
        noarksakType.setSaAnsvinit(sm.getSaksansvarlig());
        noarksakType.setSaAdmkort(sm.getAdministrativEnhet());
        noarksakType.setSaOfftittel(sm.getOffentligTittel());
        noarksakType.setSaId(sm.getSystemID());
        ofNullable(sm.getSaksdato()).map(DateTimeUtil::toString).ifPresent(noarksakType::setSaDato);
        noarksakType.setSaTittel(sm.getTittel());
        ofNullable(sm.getSaksstatus()).map(SaksstatusMapper::getNoarkType).ifPresent(noarksakType::setSaStatus);
        ofNullable(sm.getJournalenhet()).ifPresent(noarksakType::setSaJenhet);
        if (!sm.getReferanseArkivdel().isEmpty()) {
            noarksakType.setSaArkdel(sm.getReferanseArkivdel().get(0));
        }

        JournpostType journpostType = of.createJournpostType();
        ofNullable(jp.getSystemID()).ifPresent(journpostType::setJpId);
        ofNullable(jp.getTittel()).ifPresent(journpostType::setJpInnhold);
        ofNullable(jp.getJournalaar()).map(BigInteger::toString).ifPresent(journpostType::setJpJaar);
        ofNullable(jp.getJournalsekvensnummer()).map(BigInteger::toString).ifPresent(journpostType::setJpSeknr);
        ofNullable(jp.getJournalpostnummer()).map(BigInteger::toString).ifPresent(journpostType::setJpJpostnr);
        ofNullable(jp.getJournaldato()).map(DateTimeUtil::toString).ifPresent(journpostType::setJpJdato);
        ofNullable(jp.getJournaldato()).map(DateTimeUtil::toString).ifPresent(journpostType::setJpJdato);
        ofNullable(jp.getForfallsdato()).map(DateTimeUtil::toString).ifPresent(journpostType::setJpForfdato);
        ofNullable(jp.getJournalposttype()).map(JournalposttypeMapper::getNoarkType).ifPresent(journpostType::setJpNdoktype);
        ofNullable(jp.getDokumentetsDato()).map(DateTimeUtil::toString).ifPresent(journpostType::setJpDokdato);
        ofNullable(jp.getJournalstatus()).map(JournalstatusMapper::getNoarkType).ifPresent(journpostType::setJpStatus);
        ofNullable(jp.getReferanseArkivdel()).ifPresent(journpostType::setJpArkdel);
        ofNullable(jp.getAntallVedlegg()).map(BigInteger::toString).ifPresent(journpostType::setJpAntved);
        ofNullable(jp.getOffentligTittel()).ifPresent(journpostType::setJpOffinnhold);
        ofNullable(sm.getSkjerming()).map(Skjerming::getSkjermingshjemmel).ifPresent(journpostType::setJpUoff);
        ofNullable(sm.getSaksaar()).map(BigInteger::toString).ifPresent(journpostType::setJpSaar);
        ofNullable(sm.getSakssekvensnummer()).map(BigInteger::toString).ifPresent(journpostType::setJpSaseknr);

        jp.getKorrespondansepart().forEach(k -> {
            AvsmotType avsmotType = of.createAvsmotType();
            avsmotType.setAmNavn(k.getKorrespondansepartNavn());
            if ("avsender".equalsIgnoreCase(k.getKorrespondanseparttype().value())) {
                avsmotType.setAmIhtype("0");
            }
            if ("mottaker".equalsIgnoreCase(k.getKorrespondanseparttype().value())) {
                avsmotType.setAmIhtype("1");
            }

            avsmotType.setAmAdresse(String.join(" ", k.getPostadresse()));
            avsmotType.setAmPostnr(k.getPostnummer());
            avsmotType.setAmPoststed(k.getPoststed());
            avsmotType.setAmUtland(k.getLand());

            avsmotType.setAmAdmkort(k.getAdministrativEnhet());
            avsmotType.setAmSbhinit(k.getSaksbehandler());
            if (!jp.getAvskrivning().isEmpty()) {
                Avskrivning avs = jp.getAvskrivning().get(0);
                ofNullable(avs.getAvskrivningsmaate()).map(AvskrivningsmaateMapper::getNoarkType).ifPresent(avsmotType::setAmAvskm);
                ofNullable(avs.getAvskrivningsdato()).map(DateTimeUtil::toString).ifPresent(avsmotType::setAmAvskdato);
                avsmotType.setAmAvsavdok(avs.getReferanseAvskrivesAvJournalpost());
            }

            journpostType.getAvsmot().add(avsmotType);
        });

        jp.getDokumentbeskrivelseAndDokumentobjekt().stream()
                .filter(Dokumentbeskrivelse.class::isInstance)
                .map(Dokumentbeskrivelse.class::cast)
                .forEach(db -> db.getDokumentobjekt().forEach(dobj ->
                                journpostType.getDokument().add(createDokumentType(db, dobj, asic))
                        )
                );

        MeldingType meldingType = of.createMeldingType();
        meldingType.setJournpost(journpostType);
        meldingType.setNoarksak(noarksakType);

        return meldingType;
    }

    private DokumentType createDokumentType(Dokumentbeskrivelse db, Dokumentobjekt dobj, Resource asic) {
        ObjectFactory of = new ObjectFactory();
        DokumentType dokumentType = of.createDokumentType();
        String filename = dobj.getReferanseDokumentfil();
        dokumentType.setVeFilnavn(filename);
        dokumentType.setDbTittel(db.getTittel());
        dokumentType.setDlRnr(db.getDokumentnummer().toString());
        dokumentType.setDlType(TilknyttetRegistreringSomMapper.getNoarkType(db.getTilknyttetRegistreringSom()));

        String[] split = dobj.getReferanseDokumentfil().split(".");
        String ext = Stream.of(split).reduce((p, e) -> e).orElse("pdf");
        dokumentType.setVeDokformat(ext);
        dokumentType.setVeMimeType(getMimetype(ext));
        dokumentType.setVeVariant(VariantformatMapper.getNoarkType(dobj.getVariantformat()));

        FilType filType = of.createFilType();
        try {
            filType.setBase64(getFileFromAsic(filename, asic));
        } catch (IOException e) {
            throw new MeldingsUtvekslingRuntimeException(String.format("Error getting file %s from ASiC", filename), e);
        }
        dokumentType.setFil(filType);

        return dokumentType;
    }

    private byte[] getFileFromAsic(String fileName, Resource asic) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(StreamUtils.nonClosing(asic.getInputStream()))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (fileName.equals(entry.getName())) {
                    return IOUtils.toByteArray(zipInputStream);
                }
            }
        }
        throw new MeldingsUtvekslingRuntimeException(String.format("File %s is missing from ASiC", fileName));
    }
}

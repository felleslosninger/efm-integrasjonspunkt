package no.difi.meldingsutveksling.noarkexchange;

import lombok.experimental.UtilityClass;
import no.arkivverket.standarder.noark5.arkivmelding.*;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.arkivmelding.AvskrivningsmaateMapper;
import no.difi.meldingsutveksling.domain.arkivmelding.JournalposttypeMapper;
import no.difi.meldingsutveksling.domain.arkivmelding.TilknyttetRegistreringSomMapper;
import no.difi.meldingsutveksling.domain.arkivmelding.VariantformatMapper;
import no.difi.meldingsutveksling.noarkexchange.schema.core.*;
import no.difi.meldingsutveksling.noarkexchange.schema.core.ObjectFactory;
import org.apache.commons.io.IOUtils;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Optional.ofNullable;
import static no.difi.meldingsutveksling.MimeTypeExtensionMapper.getMimetype;

@UtilityClass
public class MeldingFactory {

    public static MeldingType create(Arkivmelding am, byte[] asic) {
        Saksmappe sm = ArkivmeldingUtil.getSaksmappe(am);
        Journalpost jp = ArkivmeldingUtil.getJournalpost(am);

        ObjectFactory of = new ObjectFactory();
        NoarksakType noarksakType = of.createNoarksakType();
        ofNullable(sm.getSaksaar()).map(BigInteger::toString).ifPresent(noarksakType::setSaSaar);
        ofNullable(sm.getSakssekvensnummer()).map(BigInteger::toString).ifPresent(noarksakType::setSaSeknr);
        noarksakType.setSaAnsvinit(sm.getSaksansvarlig());
        noarksakType.setSaAdmkort(sm.getAdministrativEnhet());
        noarksakType.setSaOfftittel(sm.getOffentligTittel());

        JournpostType journpostType = of.createJournpostType();
        ofNullable(jp.getJournalaar()).map(BigInteger::toString).ifPresent(journpostType::setJpJaar);
        ofNullable(jp.getJournalsekvensnummer()).map(BigInteger::toString).ifPresent(journpostType::setJpSeknr);
        ofNullable(jp.getJournalpostnummer()).map(BigInteger::toString).ifPresent(journpostType::setJpJpostnr);
        ofNullable(jp.getJournaldato()).map(XMLGregorianCalendar::toString).ifPresent(journpostType::setJpJdato);
        ofNullable(jp.getJournalposttype()).map(JournalposttypeMapper::getNoarkType).ifPresent(journpostType::setJpNdoktype);
        ofNullable(jp.getDokumentetsDato()).map(XMLGregorianCalendar::toString).ifPresent(journpostType::setJpDokdato);
        ofNullable(sm.getSkjerming()).map(Skjerming::getSkjermingshjemmel).ifPresent(journpostType::setJpUoff);


        jp.getKorrespondansepart().forEach(k -> {
            AvsmotType avsmotType = of.createAvsmotType();
            avsmotType.setAmNavn(k.getKorrespondansepartNavn());
            if ("avsender".equalsIgnoreCase(k.getKorrespondanseparttype().value())) {
                avsmotType.setAmIhtype("0");
            }
            if ("mottaker".equalsIgnoreCase(k.getKorrespondanseparttype().value())) {
                avsmotType.setAmIhtype("1");
            }
            avsmotType.setAmAdmkort(k.getAdministrativEnhet());
            avsmotType.setAmSbhinit(k.getSaksbehandler());
            if (!jp.getAvskrivning().isEmpty()) {
                Avskrivning avs = jp.getAvskrivning().get(0);
                ofNullable(avs.getAvskrivningsmaate()).map(AvskrivningsmaateMapper::getNoarkType).ifPresent(avsmotType::setAmAvskm);
                ofNullable(avs.getAvskrivningsdato()).map(XMLGregorianCalendar::toString).ifPresent(avsmotType::setAmAvskdato);
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

    private DokumentType createDokumentType(Dokumentbeskrivelse db, Dokumentobjekt dobj, byte[] asic) {
        ObjectFactory of = new ObjectFactory();
        DokumentType dokumentType = of.createDokumentType();
        String filename = dobj.getReferanseDokumentfil();
        dokumentType.setVeFilnavn(filename);
        dokumentType.setDbTittel(db.getTittel());
        dokumentType.setDlRnr(db.getDokumentnummer().toString());
        dokumentType.setDlType(TilknyttetRegistreringSomMapper.getNoarkType(db.getTilknyttetRegistreringSom()));

        String[] split = dobj.getReferanseDokumentfil().split(".");
        String ext = Stream.of(split).reduce((p, e) -> e).orElse("pdf");
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

    private byte[] getFileFromAsic(String fileName, byte[] bytes) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
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

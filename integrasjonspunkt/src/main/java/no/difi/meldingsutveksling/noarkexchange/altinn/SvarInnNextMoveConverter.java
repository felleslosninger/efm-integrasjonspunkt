package no.difi.meldingsutveksling.noarkexchange.altinn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.arkivverket.standarder.noark5.arkivmelding.Journalpost;
import no.arkivverket.standarder.noark5.arkivmelding.Korrespondansepart;
import no.arkivverket.standarder.noark5.arkivmelding.Saksmappe;
import no.arkivverket.standarder.noark5.metadatakatalog.Korrespondanseparttype;
import no.difi.meldingsutveksling.*;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.service.SBDFactory;
import no.difi.meldingsutveksling.domain.NextMoveStreamedFile;
import no.difi.meldingsutveksling.domain.StreamedFile;
import no.difi.meldingsutveksling.domain.arkivmelding.JournalposttypeMapper;
import no.difi.meldingsutveksling.domain.arkivmelding.JournalstatusMapper;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.ks.svarinn.Forsendelse;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnService;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingMessage;
import no.difi.meldingsutveksling.nextmove.AsicHandler;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.noarkexchange.MessageContext;
import no.difi.meldingsutveksling.noarkexchange.MessageContextFactory;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;

@Slf4j
@RequiredArgsConstructor
public class SvarInnNextMoveConverter {

    private final IntegrasjonspunktNokkel keyInfo;
    private final MessagePersister messagePersister;
    private final SvarInnService svarInnService;
    private final MessageContextFactory messageContextFactory;
    private final AsicHandler asicHandler;
    private final SBDFactory createSBD;
    private final IntegrasjonspunktProperties properties;

    @Transactional
    public StandardBusinessDocument convert(Forsendelse forsendelse) {
        MessageContext context = messageContextFactory.from(forsendelse.getSvarSendesTil().getOrgnr(),
                forsendelse.getMottaker().getOrgnr(),
                keyInfo.getX509Certificate());
        StandardBusinessDocument sbd = createSBD.createNextMoveSBD(
                context.getAvsender().getOrgNummer(),
                context.getMottaker().getOrgNummer(),
                forsendelse.getId(),
                forsendelse.getId(),
                properties.getFiks().getInn().getProcess(),
                DocumentType.ARKIVMELDING,
                new ArkivmeldingMessage());
        NextMoveStreamedFile arkivmeldingFile = getArkivmeldingFile(forsendelse);

        Stream<StreamedFile> attachments = Stream.concat(
                Stream.of(arkivmeldingFile),
                svarInnService.getAttachments(forsendelse));

        try (InputStream asicStream = asicHandler.archiveAndEncryptAttachments(arkivmeldingFile, attachments, context, ServiceIdentifier.DPF)) {
            messagePersister.writeStream(forsendelse.getId(), NextMoveConsts.ASIC_FILE, asicStream, -1);
        } catch (IOException e) {
            throw new NextMoveRuntimeException("Failed to create ASIC", e);
        }

        return sbd;
    }

    private NextMoveStreamedFile getArkivmeldingFile(Forsendelse forsendelse) {
        ByteArrayInputStream arkivmeldingStream = getArkivmeldingStream(forsendelse);
        return new NextMoveStreamedFile(NextMoveConsts.ARKIVMELDING_FILE, arkivmeldingStream, MediaType.APPLICATION_XML_VALUE);
    }

    private ByteArrayInputStream getArkivmeldingStream(Forsendelse forsendelse) {
        Arkivmelding arkivmelding = toArkivmelding(forsendelse);
        byte[] arkivmeldingBytes;
        try {
            arkivmeldingBytes = ArkivmeldingUtil.marshalArkivmelding(arkivmelding);
        } catch (JAXBException e) {
            log.error("Error marshalling arkivmelding", e);
            throw new NextMoveRuntimeException("Error marshalling arkivmelding");
        }

        return new ByteArrayInputStream(arkivmeldingBytes);
    }

    private Arkivmelding toArkivmelding(Forsendelse forsendelse) {
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
        if (!isNullOrEmpty(metadata.getJournaldato())) {
            journalpost.setJournaldato(DateTimeUtil.toXMLGregorianCalendar(Long.valueOf(metadata.getJournaldato())));
        }
        if (!isNullOrEmpty(metadata.getDokumentetsDato())) {
            journalpost.setDokumentetsDato(DateTimeUtil.toXMLGregorianCalendar(Long.valueOf(metadata.getDokumentetsDato())));
        }
        journalpost.setOffentligTittel(metadata.getTittel());

        saksmappe.getBasisregistrering().add(journalpost);
        Arkivmelding arkivmelding = of.createArkivmelding();
        arkivmelding.getMappe().add(saksmappe);

        return arkivmelding;
    }

}

package no.difi.meldingsutveksling.nextmove;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.*;
import no.arkivverket.standarder.noark5.metadatakatalog.Korrespondanseparttype;
import no.difi.meldingsutveksling.DateTimeUtil;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.api.AsicHandler;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.arkivmelding.JournalposttypeMapper;
import no.difi.meldingsutveksling.domain.arkivmelding.JournalstatusMapper;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.fiks.svarinn.SvarInnPackage;
import no.difi.meldingsutveksling.ks.svarinn.Forsendelse;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnService;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import no.difi.meldingsutveksling.sbd.ScopeFactory;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.io.pipe.Reject;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.bind.JAXBException;
import java.math.BigInteger;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.TimeZone;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
public class SvarInnNextMoveConverter {

    private final SvarInnService svarInnService;
    private final AsicHandler asicHandler;
    private final SBDFactory createSBD;
    private final IntegrasjonspunktProperties properties;
    private final KeystoreHelper keystoreHelper;
    private final ArkivmeldingUtil arkivmeldingUtil;
    private final UUIDGenerator uuidGenerator;

    @Transactional
    public SvarInnPackage convert(Forsendelse forsendelse, Reject reject) {
        StandardBusinessDocument sbd = createSBD.createNextMoveSBD(
                Iso6523.of(ICD.NO_ORG, hasText(forsendelse.getSvarSendesTil().getOrgnr()) ?
                        forsendelse.getSvarSendesTil().getOrgnr() :
                        properties.getFiks().getInn().getFallbackSenderOrgNr()),
                Iso6523.of(ICD.NO_ORG, forsendelse.getMottaker().getOrgnr()),
                forsendelse.getId(),
                forsendelse.getId(),
                properties.getFiks().getInn().getProcess(),
                properties.getFiks().getInn().getDocumentType(),
                new ArkivmeldingMessage());
        if (!Strings.isNullOrEmpty(forsendelse.getSvarPaForsendelse())) {
            sbd.addScope(ScopeFactory.fromRef(ScopeType.RECEIVER_REF, forsendelse.getSvarPaForsendelse()));
        }

        Resource asic = asicHandler.createCmsEncryptedAsice(
                NextMoveOutMessage.of(sbd, ServiceIdentifier.DPF),
                getArkivmeldingFile(forsendelse),
                svarInnService.getAttachments(forsendelse, reject),
                keystoreHelper.getX509Certificate(), reject);

        return new SvarInnPackage(sbd, asic);
    }

    private Document getArkivmeldingFile(Forsendelse forsendelse) {
        return Document.builder()
                .filename(NextMoveConsts.ARKIVMELDING_FILE)
                .mimeType(MediaType.APPLICATION_XML_VALUE)
                .resource(getArkivmelding(forsendelse))
                .build();
    }

    ByteArrayResource getArkivmelding(Forsendelse forsendelse) {
        Arkivmelding arkivmelding = toArkivmelding(forsendelse);
        byte[] arkivmeldingBytes;
        try {
            arkivmeldingBytes = arkivmeldingUtil.marshalArkivmelding(arkivmelding);
        } catch (JAXBException e) {
            log.error("Error marshalling arkivmelding", e);
            throw new NextMoveRuntimeException("Error marshalling arkivmelding");
        }

        return new ByteArrayResource(arkivmeldingBytes);
    }

    private Arkivmelding toArkivmelding(Forsendelse forsendelse) {
        no.arkivverket.standarder.noark5.arkivmelding.ObjectFactory of = new no.arkivverket.standarder.noark5.arkivmelding.ObjectFactory();

        Journalpost journalpost = of.createJournalpost();
        journalpost.setSystemID(uuidGenerator.generate());
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
        saksmappe.setSystemID(uuidGenerator.generate());
        Forsendelse.MetadataFraAvleverendeSystem metadata = forsendelse.getMetadataFraAvleverendeSystem();
        saksmappe.setSakssekvensnummer(BigInteger.valueOf(metadata.getSakssekvensnummer()));
        saksmappe.setSaksaar(BigInteger.valueOf(metadata.getSaksaar()));
        saksmappe.setSaksansvarlig(metadata.getSaksBehandler());

        journalpost.setJournalaar(BigInteger.valueOf(Long.parseLong(metadata.getJournalaar())));
        journalpost.setJournalsekvensnummer(BigInteger.valueOf(Long.parseLong(metadata.getJournalsekvensnummer())));
        journalpost.setJournalpostnummer(BigInteger.valueOf(Long.parseLong(metadata.getJournalpostnummer())));
        journalpost.setJournalposttype(JournalposttypeMapper.getArkivmeldingType(metadata.getJournalposttype()));
        journalpost.setJournalstatus(JournalstatusMapper.getArkivmeldingType(metadata.getJournalstatus()));

        if (!isNullOrEmpty(metadata.getJournaldato())) {
            journalpost.setJournaldato(DateTimeUtil.toXMLGregorianCalendar(Long.parseLong(metadata.getJournaldato())));
        } else {
            journalpost.setJournaldato(DateTimeUtil.toXMLGregorianCalendar(new GregorianCalendar(TimeZone.getDefault())));
        }

        if (!isNullOrEmpty(metadata.getDokumentetsDato())) {
            journalpost.setDokumentetsDato(DateTimeUtil.toXMLGregorianCalendar(Long.parseLong(metadata.getDokumentetsDato())));
        }

        if (!isNullOrEmpty(sst.getFnr())) {
            journalpost.setTittel(getTittel(forsendelse) + " (eDialog fra "+sst.getFnr()+")");
        } else {
            journalpost.setTittel(getTittel(forsendelse));
        }

        forsendelse.getFilmetadata().forEach(fmd -> {
            Dokumentbeskrivelse db = of.createDokumentbeskrivelse();
            db.setSystemID(uuidGenerator.generate());
            db.setTittel(fmd.getFilnavn());

            Dokumentobjekt dobj = of.createDokumentobjekt();
            dobj.setReferanseDokumentfil(fmd.getFilnavn());
            db.getDokumentobjekt().add(dobj);

            journalpost.getDokumentbeskrivelseAndDokumentobjekt().add(db);
        });

        saksmappe.getBasisregistrering().add(journalpost);
        Arkivmelding arkivmelding = of.createArkivmelding();
        arkivmelding.getMappe().add(saksmappe);

        arkivmelding.setSystem("Integrasjonspunkt");
        arkivmelding.setMeldingId(forsendelse.getId());
        arkivmelding.setTidspunkt(Optional.ofNullable(metadata.getDokumentetsDato())
                .map(DateTimeUtil::toXMLGregorianCalendar)
                .map(DateTimeUtil::atStartOfDay)
                .orElse(DateTimeUtil.toXMLGregorianCalendar(new GregorianCalendar(TimeZone.getDefault()))));

        return arkivmelding;
    }

    private String getTittel(Forsendelse forsendelse) {
        if (!isNullOrEmpty(forsendelse.getMetadataFraAvleverendeSystem().getTittel())) {
            return forsendelse.getMetadataFraAvleverendeSystem().getTittel();
        }
        return forsendelse.getTittel();
    }

}

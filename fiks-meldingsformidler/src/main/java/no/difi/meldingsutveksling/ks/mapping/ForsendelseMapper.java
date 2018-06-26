package no.difi.meldingsutveksling.ks.mapping;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.*;
import no.arkivverket.standarder.noark5.metadatakatalog.Korrespondanseparttype;
import no.difi.meldingsutveksling.MimeTypeExtensionMapper;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreConverter;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.ks.mapping.edu.FileTypeHandler;
import no.difi.meldingsutveksling.ks.mapping.edu.FileTypeHandlerFactory;
import no.difi.meldingsutveksling.ks.svarut.*;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.noarkexchange.schema.core.AvsmotType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;

@Slf4j
public class ForsendelseMapper {

    private IntegrasjonspunktProperties properties;
    private ServiceRegistryLookup serviceRegistry;
    private MessagePersister persister;

    public ForsendelseMapper(IntegrasjonspunktProperties properties,
                             ServiceRegistryLookup serviceRegistry,
                             MessagePersister persister) {
        this.properties = properties;
        this.serviceRegistry = serviceRegistry;
        this.persister = persister;
    }

    public SendForsendelseMedId mapFrom(ConversationResource cr, X509Certificate certificate) {
        Forsendelse.Builder<Void> forsendelse = Forsendelse.builder();
        forsendelse.withEksternref(cr.getConversationId());
        forsendelse.withKunDigitalLevering(false);
        forsendelse.withSvarPaForsendelse(""); // FIXME

        Arkivmelding am = cr.getArkivmelding();
        Saksmappe sm = (Saksmappe) am.getMappe().get(0);
        Journalpost jp = (Journalpost)  sm.getBasisregistrering().get(0);
        forsendelse.withTittel(jp.getOffentligTittel());

        forsendelse.withKonteringskode(properties.getFiks().getUt().getKonverteringsKode()); // FIXME: konvertering -> kontering?
        forsendelse.withKryptert(properties.getFiks().isKryptert());
        forsendelse.withAvgivendeSystem(properties.getNoarkSystem().getType()); // FIXME: nextmove?

        final InfoRecord receiverInfo = serviceRegistry.getInfoRecord(cr.getReceiverId());
        forsendelse.withMottaker(mottakerFrom(receiverInfo));
        Optional<Korrespondansepart> avsender = jp.getKorrespondansepart().stream()
                .filter(k -> k.getKorrespondanseparttype().equals(Korrespondanseparttype.AVSENDER))
                .findFirst();
        if (avsender.isPresent()) {
            forsendelse.withSvarSendesTil(mottakerFrom(avsender.get(), cr.getSenderId()));
        } else {
            final InfoRecord senderInfo = serviceRegistry.getInfoRecord(cr.getSenderId());
            forsendelse.withSvarSendesTil(mottakerFrom(senderInfo));
        }

        forsendelse.withMetadataFraAvleverendeSystem(metaDataFrom(sm));
        forsendelse.withDokumenter(mapArkivmeldingDokumenter(cr, jp.getDokumentbeskrivelseAndDokumentobjekt()));
        String senderRef;
        if (!isNullOrEmpty(am.getMeldingId())) {
            senderRef = am.getMeldingId();
        } else {
            senderRef = cr.getConversationId();
        }

        return SendForsendelseMedId.builder()
                .withForsendelse(forsendelse.build())
                .withForsendelsesid(senderRef).build();
    }

    public SendForsendelseMedId mapFrom(EDUCore eduCore, X509Certificate certificate) {
        final Forsendelse.Builder<Void> forsendelse = Forsendelse.builder();
        forsendelse.withEksternref(eduCore.getId());
        forsendelse.withKunDigitalLevering(false);
        forsendelse.withSvarPaForsendelse(eduCore.getReceiver().getRef());

        final MeldingType meldingType = EDUCoreConverter.payloadAsMeldingType(eduCore.getPayload());
        forsendelse.withTittel(meldingType.getJournpost().getJpOffinnhold());

        final FileTypeHandlerFactory fileTypeHandlerFactory = new FileTypeHandlerFactory(properties.getFiks(), certificate);
        forsendelse.withDokumenter(mapFrom(meldingType.getJournpost().getDokument(), fileTypeHandlerFactory));

        forsendelse.withKonteringskode(properties.getFiks().getUt().getKonverteringsKode());
        forsendelse.withKryptert(properties.getFiks().isKryptert());
        forsendelse.withAvgivendeSystem(properties.getNoarkSystem().getType());

        forsendelse.withPrintkonfigurasjon(Printkonfigurasjon.builder()
                .withTosidig(true)
                .withFargePrint(false)
                .withBrevtype(Brevtype.BPOST).build());

        final InfoRecord receiverInfo = serviceRegistry.getInfoRecord(eduCore.getReceiver().getIdentifier());
        forsendelse.withMottaker(mottakerFrom(receiverInfo));

        Optional<AvsmotType> avsender = getAvsender(meldingType);
        if (avsender.isPresent()) {
            forsendelse.withSvarSendesTil(mottakerFrom(avsender.get(), receiverInfo.getIdentifier()));
        } else {
            final InfoRecord senderInfo = serviceRegistry.getInfoRecord(eduCore.getSender().getIdentifier());
            forsendelse.withSvarSendesTil(mottakerFrom(senderInfo));
        }

        forsendelse.withMetadataFraAvleverendeSystem(metaDataFrom(meldingType));
        String senderRef;
        if (isNullOrEmpty(eduCore.getSender().getRef())) {
            log.warn("No envelope.sender.ref in message, using conversationId instead..");
            senderRef = eduCore.getId();
        } else {
            senderRef = eduCore.getSender().getRef();
        }

        return SendForsendelseMedId.builder()
                .withForsendelse(forsendelse.build())
                .withForsendelsesid(senderRef)
                .build();
    }

    private NoarkMetadataFraAvleverendeSakssystem metaDataFrom(Saksmappe sm) {
        NoarkMetadataFraAvleverendeSakssystem.Builder<Void> metadata = NoarkMetadataFraAvleverendeSakssystem.builder();
        Journalpost jp = (Journalpost)  sm.getBasisregistrering().get(0);

        metadata.withSakssekvensnummer(sm.getSakssekvensnummer().intValueExact());
        metadata.withSaksaar(sm.getSaksaar().intValueExact());
        metadata.withJournalaar(jp.getJournalaar().intValueExact());
        metadata.withJournalsekvensnummer(jp.getJournalsekvensnummer().intValueExact());
        metadata.withJournalpostnummer(jp.getJournalpostnummer().intValueExact());
        metadata.withJournalposttype(jp.getJournalposttype().value());
        metadata.withJournalstatus(jp.getJournalstatus().value());
        metadata.withJournaldato(jp.getJournaldato());
        metadata.withDokumentetsDato(jp.getDokumentetsDato());
        metadata.withTittel(jp.getOffentligTittel());

        Optional<Korrespondansepart> avsender = jp.getKorrespondansepart().stream()
                .filter(k -> k.getKorrespondanseparttype().equals(Korrespondanseparttype.AVSENDER))
                .findFirst();
        avsender.map(Korrespondansepart::getSaksbehandler).ifPresent(metadata::withSaksbehandler);

        return metadata.build();
    }

    private NoarkMetadataFraAvleverendeSakssystem metaDataFrom(MeldingType meldingType) {
        NoarkMetadataFraAvleverendeSakssystem.Builder<Void> metadata = NoarkMetadataFraAvleverendeSakssystem.builder();
        metadata.withSakssekvensnummer(Integer.valueOf(meldingType.getNoarksak().getSaSeknr()));
        metadata.withSaksaar(Integer.valueOf(meldingType.getNoarksak().getSaSaar()));
        metadata.withJournalaar(Integer.valueOf(meldingType.getJournpost().getJpJaar()));
        metadata.withJournalsekvensnummer(Integer.valueOf(meldingType.getJournpost().getJpSeknr()));
        metadata.withJournalpostnummer(Integer.valueOf(meldingType.getJournpost().getJpJpostnr()));
        metadata.withJournalposttype(meldingType.getJournpost().getJpNdoktype());
        metadata.withJournalstatus(meldingType.getJournpost().getJpStatus());
        metadata.withJournaldato(journalDatoFrom(meldingType.getJournpost().getJpJdato()));
        metadata.withDokumentetsDato(journalDatoFrom(meldingType.getJournpost().getJpDokdato()));
        metadata.withTittel(meldingType.getJournpost().getJpOffinnhold());

        Optional<AvsmotType> avsender = getSaksbehandler(meldingType);
        avsender.map(a -> a.getAmNavn()).ifPresent(metadata::withSaksbehandler);

        return metadata.build();
    }


    private Optional<AvsmotType> getSaksbehandler(MeldingType meldingType) {
        List<AvsmotType> avsmotlist = meldingType.getJournpost().getAvsmot();
        return avsmotlist.stream().filter(f -> "0".equals(f.getAmIhtype())).findFirst();
    }

    private Optional<AvsmotType> getAvsender(MeldingType meldingType) {
        List<AvsmotType> avsmotlist = meldingType.getJournpost().getAvsmot();
        return avsmotlist.stream().filter(f -> "1".equals(f.getAmIhtype())).findFirst();
    }

    private XMLGregorianCalendar journalDatoFrom(String jpDato) {
        LocalDateTime localDateTime = LocalDateTime.of(LocalDate.parse(jpDato), LocalTime.of(0, 0));

        GregorianCalendar gcal = GregorianCalendar.from(localDateTime.atZone(ZoneId.systemDefault()));
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
        } catch (DatatypeConfigurationException e) {
            throw new ForsendelseMappingException("Unable to map date", e);
        }
    }

    private Adresse mottakerFrom(AvsmotType avsmotType, String orgnr) {
        Adresse.Builder<Void> mottaker = Adresse.builder();

        OrganisasjonDigitalAdresse orgAdr = OrganisasjonDigitalAdresse.builder()
                .withOrgnr(orgnr)
                .build();
        mottaker.withDigitalAdresse(orgAdr);

        PostAdresse postAdr = PostAdresse.builder()
                .withNavn(avsmotType.getAmNavn())
                .withAdresse1(avsmotType.getAmAdresse())
                .withPostnr(avsmotType.getAmPostnr())
                .withPoststed(avsmotType.getAmPoststed())
                .withLand(avsmotType.getAmUtland())
                .build();
        mottaker.withPostAdresse(postAdr);

        return mottaker.build();
    }

    private Adresse mottakerFrom(Korrespondansepart kp, String orgnr) {
        Adresse.Builder<Void> mottaker = Adresse.builder();

        OrganisasjonDigitalAdresse orgAdr = OrganisasjonDigitalAdresse.builder().withOrgnr(orgnr).build();
        mottaker.withDigitalAdresse(orgAdr);

        String adr = kp.getPostadresse().stream().collect(Collectors.joining(" "));
        PostAdresse postAdresse = PostAdresse.builder()
                .withNavn(kp.getKorrespondansepartNavn())
                .withAdresse1(adr)
                .withPostnr(kp.getPostnummer())
                .withPoststed(kp.getPoststed())
                .withLand(kp.getLand())
                .build();
        mottaker.withPostAdresse(postAdresse);

        return mottaker.build();
    }

    private Adresse mottakerFrom(InfoRecord infoRecord) {
        Adresse.Builder<Void> mottaker = Adresse.builder();

        OrganisasjonDigitalAdresse orgAdr = OrganisasjonDigitalAdresse.builder()
                .withOrgnr(infoRecord.getIdentifier())
                .build();
        mottaker.withDigitalAdresse(orgAdr);

        PostAdresse.Builder<Void> postAdr = PostAdresse.builder()
                .withNavn(infoRecord.getOrganizationName());

        if (infoRecord.getPostadresse() != null) {
            postAdr.withAdresse1(infoRecord.getPostadresse().getAdresse());
            postAdr.withPostnr(infoRecord.getPostadresse().getPostnummer());
            postAdr.withPoststed(infoRecord.getPostadresse().getPoststed());
            postAdr.withLand(infoRecord.getPostadresse().getLand());
        } else {
            postAdr.withPostnr("0192");
            postAdr.withPoststed("Oslo");
            postAdr.withLand("Norge");
        }
        mottaker.withPostAdresse(postAdr.build());

        return mottaker.build();
    }

    private List<Dokument> mapArkivmeldingDokumenter(ConversationResource cr, List<Object> docs) {
        List<Dokument> dokumenter = Lists.newArrayList();

        for (Object d : docs) {
            if (d instanceof Dokumentbeskrivelse) {
                Dokumentbeskrivelse db = (Dokumentbeskrivelse) d;
                db.getDokumentobjekt().forEach(dbo -> {
                    String f = dbo.getReferanseDokumentfil();
                    try {
                        byte[] bytes = persister.read(cr, f);
                        String[] split = dbo.getReferanseDokumentfil().split(".");
                        String ext = Stream.of(split).reduce((p, e) -> e).orElse("pdf");
                        String mimetype = MimeTypeExtensionMapper.getMimetype(ext);
                        final DataHandler dataHandler = new DataHandler(new ByteArrayDataSource(new ByteArrayInputStream(bytes), mimetype));
                        Dokument dokument = Dokument.builder()
                                .withData(dataHandler)
                                .withFilnavn(f)
                                .withMimetype(mimetype)
                                .build();
                        dokumenter.add(dokument);
                    } catch (IOException e) {
                        throw new MeldingsUtvekslingRuntimeException(String.format("Could not load file %s", f));
                    }
                });
            }
        }

        return dokumenter;
    }

    private List<Dokument> mapFrom(List<DokumentType> dokumentTypes, FileTypeHandlerFactory fileTypeHandlerFactory) {
        List<Dokument> dokumenter = new ArrayList<>(dokumentTypes.size());
        for (DokumentType d : dokumentTypes) {
            final FileTypeHandler fileTypeHandler = fileTypeHandlerFactory.createFileTypeHandler(d);
            Dokument.Builder dokumentBuilder = fileTypeHandler.map(Dokument.builder());

            dokumentBuilder.withFilnavn(d.getVeFilnavn());
            dokumentBuilder.withMimetype(d.getVeMimeType());

            dokumenter.add(dokumentBuilder.build());
        }

        return dokumenter;
    }


}

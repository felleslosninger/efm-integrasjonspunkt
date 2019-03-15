package no.difi.meldingsutveksling.ks.mapping;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.*;
import no.arkivverket.standarder.noark5.metadatakatalog.Korrespondanseparttype;
import no.difi.meldingsutveksling.InputStreamDataSource;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingException;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreConverter;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.ks.mapping.edu.FileTypeHandler;
import no.difi.meldingsutveksling.ks.mapping.edu.FileTypeHandlerFactory;
import no.difi.meldingsutveksling.ks.svarut.*;
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.noarkexchange.schema.core.AvsmotType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecordWrapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import javax.activation.DataHandler;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.NextMoveConsts.ARKIVMELDING_FILE;

@Slf4j
@Component
public class FiksMapper {

    private IntegrasjonspunktProperties properties;
    private ServiceRegistryLookup serviceRegistry;
    private MessagePersister messagePersister;

    public FiksMapper(IntegrasjonspunktProperties properties,
                      ServiceRegistryLookup serviceRegistry,
                      ObjectProvider<MessagePersister> messagePersister) {
        this.properties = properties;
        this.serviceRegistry = serviceRegistry;
        this.messagePersister = messagePersister.getIfUnique();
    }

    public SendForsendelseMedId mapFrom(NextMoveMessage message, X509Certificate certificate) throws NextMoveException, ArkivmeldingException {
        final Forsendelse.Builder<Void> forsendelse = Forsendelse.builder();
        forsendelse.withEksternref(message.getConversationId());
        forsendelse.withKunDigitalLevering(false);
        // TODO Scope.ReceiverRef?
        forsendelse.withSvarPaForsendelse(UUID.randomUUID().toString());

        // Process arkivmelding
        BusinessMessageFile bmf = message.getFiles().stream()
                .filter(f -> ARKIVMELDING_FILE.equals(f.getFilename()))
                .findAny()
                .orElseThrow(() -> new NextMoveException(format("No attachement \"%s\" found", ARKIVMELDING_FILE)));
        InputStream is = messagePersister.readStream(message.getConversationId(), bmf.getIdentifier()).getInputStream();
        Arkivmelding am;
        try {
            am = ArkivmeldingUtil.unmarshalArkivmelding(is);
        } catch (JAXBException e) {
            throw new NextMoveException("Error unmarshalling arkivmelding", e);
        }

        Saksmappe saksmappe = am.getMappe().stream()
                .filter(Saksmappe.class::isInstance)
                .map(Saksmappe.class::cast)
                .findFirst()
                .orElseThrow(() -> new ArkivmeldingException("No \"Saksmappe\" found in Arkivmelding"));
        Journalpost journalpost = saksmappe.getBasisregistrering().stream()
                .filter(Journalpost.class::isInstance)
                .map(Journalpost.class::cast)
                .findFirst()
                .orElseThrow(() -> new ArkivmeldingException("No \"Journalpost\" found in Arkivmelding"));
        forsendelse.withTittel(journalpost.getOffentligTittel());

        ServiceRecordWrapper serviceRecord = serviceRegistry.getServiceRecord(message.getReceiverIdentifier());
        Integer dpfSecurityLevel = serviceRecord.getSecuritylevels().get(ServiceIdentifier.DPF);
        if (dpfSecurityLevel != null && dpfSecurityLevel == 4) {
            forsendelse.withKrevNiva4Innlogging(true);
        }

        forsendelse.withKonteringskode(properties.getFiks().getUt().getKonteringsKode());
        forsendelse.withKryptert(properties.getFiks().isKryptert());
        forsendelse.withAvgivendeSystem(properties.getNoarkSystem().getType());

        forsendelse.withPrintkonfigurasjon(Printkonfigurasjon.builder()
                .withTosidig(true)
                .withFargePrint(false)
                .withBrevtype(Brevtype.BPOST).build());

        final InfoRecord receiverInfo = serviceRegistry.getInfoRecord(message.getReceiverIdentifier());
        forsendelse.withMottaker(mottakerFrom(receiverInfo));

        Optional<Korrespondansepart> avsender = journalpost.getKorrespondansepart().stream()
                .filter(k -> k.getKorrespondanseparttype().equals(Korrespondanseparttype.AVSENDER))
                .findFirst();

        forsendelse.withSvarSendesTil(
                avsender.map(a -> mottakerFrom(a, message.getSenderIdentifier()))
                        .orElseGet(() -> mottakerFrom(serviceRegistry.getInfoRecord(message.getSenderIdentifier())))
        );

        forsendelse.withMetadataFraAvleverendeSystem(metaDataFrom(saksmappe, journalpost));
        Set<Dokumentbeskrivelse> dokumentbeskrivelser = journalpost.getDokumentbeskrivelseAndDokumentobjekt().stream()
                .filter(Dokumentbeskrivelse.class::isInstance)
                .map(Dokumentbeskrivelse.class::cast)
                .collect(Collectors.toSet());
        forsendelse.withDokumenter(mapArkivmeldingDokumenter(message, dokumentbeskrivelser, certificate));

        return SendForsendelseMedId.builder()
                .withForsendelse(forsendelse.build())
                // TODO Scope.SenderRef
                .withForsendelsesid(message.getConversationId())
                .build();
    }

    public SendForsendelseMedId mapFrom(EDUCore eduCore, X509Certificate certificate) {
        final Forsendelse.Builder<Void> forsendelse = Forsendelse.builder();
        forsendelse.withEksternref(eduCore.getId());
        forsendelse.withKunDigitalLevering(false);
        String receiverRef = eduCore.getReceiver().getRef();
        if (!Strings.isNullOrEmpty(receiverRef)) {
            try {
                UUID.fromString(receiverRef);
            } catch (IllegalArgumentException e) {
                log.warn("receiver.ref={} is not valid UUID, setting blank value", receiverRef, e);
                receiverRef = null;
            }
        }
        forsendelse.withSvarPaForsendelse(receiverRef);

        final MeldingType meldingType = EDUCoreConverter.payloadAsMeldingType(eduCore.getPayload());
        forsendelse.withTittel(meldingType.getJournpost().getJpOffinnhold());

        final FileTypeHandlerFactory fileTypeHandlerFactory = new FileTypeHandlerFactory(properties.getFiks(), certificate);
        forsendelse.withDokumenter(mapFrom(meldingType.getJournpost().getDokument(), fileTypeHandlerFactory));

        ServiceRecordWrapper serviceRecord = serviceRegistry.getServiceRecord(eduCore.getReceiver().getIdentifier());
        Integer dpfSecurityLevel = serviceRecord.getSecuritylevels().get(ServiceIdentifier.DPF);
        if (dpfSecurityLevel != null && dpfSecurityLevel == 4) {
            forsendelse.withKrevNiva4Innlogging(true);
        }

        forsendelse.withKonteringskode(properties.getFiks().getUt().getKonteringsKode());
        forsendelse.withKryptert(properties.getFiks().isKryptert());
        forsendelse.withAvgivendeSystem(properties.getNoarkSystem().getType());

        forsendelse.withPrintkonfigurasjon(Printkonfigurasjon.builder()
                .withTosidig(true)
                .withFargePrint(false)
                .withBrevtype(Brevtype.BPOST).build());

        final InfoRecord receiverInfo = serviceRegistry.getInfoRecord(eduCore.getReceiver().getIdentifier());
        forsendelse.withMottaker(mottakerFrom(receiverInfo));

        final InfoRecord senderInfo = serviceRegistry.getInfoRecord(eduCore.getSender().getIdentifier());
        forsendelse.withSvarSendesTil(mottakerFrom(senderInfo));

        forsendelse.withMetadataFraAvleverendeSystem(metaDataFrom(meldingType));
        String senderRef;
        if (Strings.isNullOrEmpty(eduCore.getSender().getRef())) {
            log.warn("No envelope.sender.ref in message, using conversationId instead..");
            senderRef = eduCore.getId();
        } else {
            senderRef = eduCore.getSender().getRef();
            log.debug("sender.ref={}, validating", senderRef);
            try {
                UUID.fromString(senderRef);
            } catch (IllegalArgumentException e) {
                log.warn("sender.ref={} is not valid UUID, using conversationId={} as forsendelsesId", senderRef, eduCore.getId(), e);
                senderRef = eduCore.getId();
            }
        }

        return SendForsendelseMedId.builder()
                .withForsendelse(forsendelse.build())
                .withForsendelsesid(senderRef)
                .build();
    }

    private Set<Dokument> mapArkivmeldingDokumenter(NextMoveMessage message, Set<Dokumentbeskrivelse> docs, X509Certificate cert) throws NextMoveException {
        Set<Dokument> dokumentList = Sets.newHashSet();

        for (Dokumentbeskrivelse db : docs) {
            for (Dokumentobjekt d : db.getDokumentobjekt()) {
                BusinessMessageFile file = message.getFiles().stream()
                        .filter(bmf -> bmf.getFilename().equals(d.getReferanseDokumentfil()))
                        .findFirst()
                        .orElseThrow(() -> new NextMoveException("File '%s' referenced in '%s' not found"));

                InputStream is = messagePersister.readStream(message.getConversationId(), file.getIdentifier()).getInputStream();
                PipedOutputStream pos = new PipedOutputStream();

                CompletableFuture.runAsync(() -> {
                    log.trace("Starting thread: encrypt attachement for FIKS forsendelse");
                    new CmsUtil().createCMSStreamed(is, pos, cert);
                    try {
                        pos.close();
                    } catch (IOException e) {
                        throw new NextMoveRuntimeException("Error closing attachement encryption output stream", e);
                    }
                    log.trace("Thread finished: encrypt attachement for FIKS forsendelse");
                });

                PipedInputStream pis;
                try {
                    pis = new PipedInputStream(pos);
                } catch (IOException e) {
                    throw new NextMoveException("Error creating PipedInputStream from encrypted attachement", e);
                }
                DataHandler dh = new DataHandler(InputStreamDataSource.of(pis));

                Dokument dokument = Dokument.builder()
                        .withData(dh)
                        .withFilnavn(file.getFilename())
                        .withMimetype(file.getMimetype())
                        .build();
                dokumentList.add(dokument);
            }
        }

        return dokumentList;
    }

    private NoarkMetadataFraAvleverendeSakssystem metaDataFrom(Saksmappe sm, Journalpost jp) {
        NoarkMetadataFraAvleverendeSakssystem.Builder<Void> metadata = NoarkMetadataFraAvleverendeSakssystem.builder();

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

        Optional<AvsmotType> avsender = getAvsender(meldingType);
        avsender.map(a -> a.getAmNavn()).ifPresent(metadata::withSaksbehandler);

        return metadata.build();
    }


    private Optional<AvsmotType> getAvsender(MeldingType meldingType) {
        List<AvsmotType> avsmotlist = meldingType.getJournpost().getAvsmot();
        return avsmotlist.stream().filter(f -> "0".equals(f.getAmIhtype())).findFirst();
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

package no.difi.meldingsutveksling.ks.mapping;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.*;
import no.arkivverket.standarder.noark5.metadatakatalog.Korrespondanseparttype;
import no.difi.meldingsutveksling.InputStreamDataSource;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreConverter;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.ks.mapping.edu.FileTypeHandlerFactory;
import no.difi.meldingsutveksling.ks.svarut.*;
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister;
import no.difi.meldingsutveksling.nextmove.message.FileEntryStream;
import no.difi.meldingsutveksling.noarkexchange.schema.core.AvsmotType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import no.difi.meldingsutveksling.pipes.Pipe;
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
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.NextMoveConsts.ARKIVMELDING_FILE;

@Slf4j
@Component
public class FiksMapper {

    private final IntegrasjonspunktProperties properties;
    private final ServiceRegistryLookup serviceRegistry;
    private final CryptoMessagePersister cryptoMessagePersister;
    private final UUIDGenerator uuidGenerator;
    private final ObjectProvider<CmsUtil> cmsUtilProvider;

    public FiksMapper(IntegrasjonspunktProperties properties,
                      ServiceRegistryLookup serviceRegistry,
                      CryptoMessagePersister cryptoMessagePersister,
                      UUIDGenerator uuidGenerator,
                      ObjectProvider<CmsUtil> cmsUtilProvider) {
        this.properties = properties;
        this.serviceRegistry = serviceRegistry;
        this.cryptoMessagePersister = cryptoMessagePersister;
        this.uuidGenerator = uuidGenerator;
        this.cmsUtilProvider = cmsUtilProvider;
    }

    public SendForsendelseMedId mapFrom(NextMoveMessage message, X509Certificate certificate) throws NextMoveException {
        return SendForsendelseMedId.builder()
                .withForsendelse(getForsendelse(message, certificate))
                .withForsendelsesid(message.getSbd().findScope(ScopeType.SENDER_REF).map(Scope::getIdentifier).orElse(message.getConversationId()))
                .build();
    }

    private Forsendelse getForsendelse(NextMoveMessage message, X509Certificate certificate) throws NextMoveException {
        Arkivmelding am = getArkivmelding(message);
        Saksmappe saksmappe = ArkivmeldingUtil.getSaksmappe(am);
        Journalpost journalpost = ArkivmeldingUtil.getJournalpost(am);

        return Forsendelse.builder()
                .withEksternref(message.getConversationId())
                .withKunDigitalLevering(false)
                .withSvarPaForsendelse(message.getSbd().findScope(ScopeType.RECEIVER_REF).map(Scope::getIdentifier).orElse(uuidGenerator.generate()))
                .withTittel(journalpost.getOffentligTittel())
                .withKrevNiva4Innlogging(kreverNiva4Innlogging(message))
                .withKonteringskode(properties.getFiks().getUt().getKonteringsKode())
                .withKryptert(properties.getFiks().isKryptert())
                .withAvgivendeSystem(properties.getNoarkSystem().getType())
                .withPrintkonfigurasjon(getPrintkonfigurasjon())
                .withMottaker(getMottaker(message))
                .withSvarSendesTil(getSvarSendesTil(message, journalpost))
                .withMetadataFraAvleverendeSystem(metaDataFrom(saksmappe, journalpost))
                .withDokumenter(mapArkivmeldingDokumenter(message, getDokumentbeskrivelser(journalpost), certificate))
                .build();
    }

    private Printkonfigurasjon getPrintkonfigurasjon() {
        return Printkonfigurasjon.builder()
                .withTosidig(true)
                .withFargePrint(false)
                .withBrevtype(Brevtype.BPOST).build();
    }

    private Adresse getSvarSendesTil(NextMoveMessage message, Journalpost journalpost) {
        return journalpost.getKorrespondansepart().stream()
                .filter(k -> k.getKorrespondanseparttype().equals(Korrespondanseparttype.AVSENDER))
                .map(a -> mottakerFrom(a, message.getSenderIdentifier()))
                .findFirst()
                .orElseGet(() -> mottakerFrom(serviceRegistry.getInfoRecord(message.getSenderIdentifier())));
    }

    private boolean kreverNiva4Innlogging(NextMoveMessage message) {
        ServiceRecordWrapper serviceRecord = serviceRegistry.getServiceRecord(message.getReceiverIdentifier());
        Integer dpfSecurityLevel = serviceRecord.getSecuritylevels().get(ServiceIdentifier.DPF);
        return dpfSecurityLevel != null && dpfSecurityLevel == 4;
    }

    private Set<Dokumentbeskrivelse> getDokumentbeskrivelser(Journalpost journalpost) {
        return journalpost.getDokumentbeskrivelseAndDokumentobjekt().stream()
                .filter(Dokumentbeskrivelse.class::isInstance)
                .map(Dokumentbeskrivelse.class::cast)
                .collect(Collectors.toSet());
    }

    private Adresse getMottaker(NextMoveMessage message) {
        final InfoRecord receiverInfo = serviceRegistry.getInfoRecord(message.getReceiverIdentifier());
        return mottakerFrom(receiverInfo);
    }

    private Arkivmelding getArkivmelding(NextMoveMessage message) throws NextMoveException {
        String arkivmeldingIdentifier = getArkivmeldingIdentifier(message);

        try (FileEntryStream fileEntryStream = cryptoMessagePersister.readStream(message.getConversationId(), arkivmeldingIdentifier)) {
            return ArkivmeldingUtil.unmarshalArkivmelding(fileEntryStream.getInputStream());
        } catch (JAXBException e) {
            throw new NextMoveException("Error unmarshalling arkivmelding", e);
        } catch (IOException e) {
            throw new NextMoveException("Reading failed for arkivmelding", e);
        }
    }

    private String getArkivmeldingIdentifier(NextMoveMessage message) throws NextMoveException {
        return message.getFiles().stream()
                .filter(f -> ARKIVMELDING_FILE.equals(f.getFilename()))
                .findAny()
                .map(BusinessMessageFile::getIdentifier)
                .orElseThrow(() -> new NextMoveException(format("No attachement \"%s\" found", ARKIVMELDING_FILE)));
    }

    public SendForsendelseMedId mapFrom(EDUCore eduCore, X509Certificate certificate) {
        final Forsendelse.Builder<Void> forsendelse = Forsendelse.builder()
                .withEksternref(eduCore.getId())
                .withKunDigitalLevering(false)
                .withSvarPaForsendelse(getReceiverRef(eduCore));

        final MeldingType meldingType = EDUCoreConverter.payloadAsMeldingType(eduCore.getPayload());
        forsendelse.withTittel(meldingType.getJournpost().getJpOffinnhold());

        final FileTypeHandlerFactory fileTypeHandlerFactory = new FileTypeHandlerFactory(properties.getFiks(), certificate);
        forsendelse.withDokumenter(mapFrom(meldingType.getJournpost().getDokument(), fileTypeHandlerFactory));

        ServiceRecordWrapper serviceRecord = serviceRegistry.getServiceRecord(eduCore.getReceiver().getIdentifier());
        Integer dpfSecurityLevel = serviceRecord.getSecuritylevels().get(ServiceIdentifier.DPF);
        if (dpfSecurityLevel != null && dpfSecurityLevel == 4) {
            forsendelse.withKrevNiva4Innlogging(true);
        }

        forsendelse.withKonteringskode(properties.getFiks().getUt().getKonteringsKode())
                .withKryptert(properties.getFiks().isKryptert())
                .withAvgivendeSystem(properties.getNoarkSystem().getType())
                .withPrintkonfigurasjon(getPrintkonfigurasjon());

        final InfoRecord receiverInfo = serviceRegistry.getInfoRecord(eduCore.getReceiver().getIdentifier());
        forsendelse.withMottaker(mottakerFrom(receiverInfo));

        final InfoRecord senderInfo = serviceRegistry.getInfoRecord(eduCore.getSender().getIdentifier());
        forsendelse.withSvarSendesTil(mottakerFrom(senderInfo));

        forsendelse.withMetadataFraAvleverendeSystem(metaDataFrom(meldingType));

        return SendForsendelseMedId.builder()
                .withForsendelse(forsendelse.build())
                .withForsendelsesid(getSenderRef(eduCore))
                .build();
    }

    private String getSenderRef(EDUCore eduCore) {
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
        return senderRef;
    }

    private String getReceiverRef(EDUCore eduCore) {
        String receiverRef = eduCore.getReceiver().getRef();
        if (!Strings.isNullOrEmpty(receiverRef)) {
            try {
                UUID.fromString(receiverRef);
            } catch (IllegalArgumentException e) {
                log.warn("receiver.ref={} is not valid UUID, setting blank value", receiverRef, e);
                receiverRef = null;
            }
        }
        return receiverRef;
    }

    private Set<Dokument> mapArkivmeldingDokumenter(NextMoveMessage message, Set<Dokumentbeskrivelse> docs, X509Certificate cert) {
        return docs.stream()
                .flatMap(p -> p.getDokumentobjekt().stream())
                .map(d -> getBusinessMessageFile(message, d.getReferanseDokumentfil()))
                .map(file -> getDocument(message.getConversationId(), file, cert))
                .collect(Collectors.toSet());
    }

    private BusinessMessageFile getBusinessMessageFile(NextMoveMessage message, String referanseDokumentfil) {
        return message.getFiles().stream()
                .filter(bmf -> bmf.getFilename().equals(referanseDokumentfil))
                .findFirst()
                .orElseThrow(() -> new NextMoveRuntimeException(
                        String.format("File '%s' referenced in '%s' not found", referanseDokumentfil, message.getConversationId())));
    }

    private Dokument getDocument(String conversationId, BusinessMessageFile file, X509Certificate cert) {
        try {
            FileEntryStream fileEntryStream = cryptoMessagePersister.readStream(conversationId, file.getIdentifier());

            return Dokument.builder()
                    .withData(getDataHandler(cert, fileEntryStream.getInputStream()))
                    .withFilnavn(file.getFilename())
                    .withMimetype(file.getMimetype())
                    .build();
        } catch (IOException e) {
            throw new NextMoveRuntimeException(String.format("Could not get Document for conversationId %s", conversationId));
        }
    }

    private DataHandler getDataHandler(X509Certificate cert, InputStream is) {
        PipedInputStream encrypted = Pipe.of("encrypt attachment for FIKS forsendelse",
                inlet -> cmsUtilProvider.getIfAvailable().createCMSStreamed(is, inlet, cert))
                .outlet();

        return new DataHandler(InputStreamDataSource.of(encrypted));
    }

    private NoarkMetadataFraAvleverendeSakssystem metaDataFrom(Saksmappe sm, Journalpost jp) {
        return NoarkMetadataFraAvleverendeSakssystem.builder()
                .withSakssekvensnummer(toInt(sm.getSakssekvensnummer()))
                .withSaksaar(toInt(sm.getSaksaar()))
                .withJournalaar(toInt(jp.getJournalaar()))
                .withJournalsekvensnummer(toInt(jp.getJournalsekvensnummer()))
                .withJournalpostnummer(toInt(jp.getJournalpostnummer()))
                .withJournalposttype(jp.getJournalposttype().value())
                .withJournalstatus(jp.getJournalstatus().value())
                .withJournaldato(jp.getJournaldato())
                .withDokumentetsDato(jp.getDokumentetsDato())
                .withTittel(jp.getOffentligTittel())
                .withSaksbehandler(getSaksbehandler(jp).orElse(null))
                .build();
    }

    private Optional<String> getSaksbehandler(Journalpost jp) {
        return jp.getKorrespondansepart().stream()
                .filter(k -> k.getKorrespondanseparttype().equals(Korrespondanseparttype.AVSENDER))
                .map(Korrespondansepart::getSaksbehandler)
                .findFirst();
    }

    private int toInt(BigInteger x) {
        return x == null ? 0 : x.intValueExact();
    }

    private NoarkMetadataFraAvleverendeSakssystem metaDataFrom(MeldingType meldingType) {
        return NoarkMetadataFraAvleverendeSakssystem.builder()
                .withSakssekvensnummer(Integer.valueOf(meldingType.getNoarksak().getSaSeknr()))
                .withSaksaar(Integer.valueOf(meldingType.getNoarksak().getSaSaar()))
                .withJournalaar(Integer.valueOf(meldingType.getJournpost().getJpJaar()))
                .withJournalsekvensnummer(Integer.valueOf(meldingType.getJournpost().getJpSeknr()))
                .withJournalpostnummer(Integer.valueOf(meldingType.getJournpost().getJpJpostnr()))
                .withJournalposttype(meldingType.getJournpost().getJpNdoktype())
                .withJournalstatus(meldingType.getJournpost().getJpStatus())
                .withJournaldato(journalDatoFrom(meldingType.getJournpost().getJpJdato()))
                .withDokumentetsDato(journalDatoFrom(meldingType.getJournpost().getJpDokdato()))
                .withTittel(meldingType.getJournpost().getJpOffinnhold())
                .withSaksbehandler(getAvsender(meldingType).orElse(null))
                .build();
    }


    private Optional<String> getAvsender(MeldingType meldingType) {
        return meldingType.getJournpost().getAvsmot()
                .stream()
                .filter(f -> "0".equals(f.getAmIhtype()))
                .map(AvsmotType::getAmNavn)
                .findFirst();
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
        return Adresse.builder()
                .withDigitalAdresse(OrganisasjonDigitalAdresse.builder().withOrgnr(orgnr).build())
                .withPostAdresse(PostAdresse.builder()
                        .withNavn(kp.getKorrespondansepartNavn())
                        .withAdresse1(String.join(" ", kp.getPostadresse()))
                        .withPostnr(kp.getPostnummer())
                        .withPoststed(kp.getPoststed())
                        .withLand(kp.getLand())
                        .build())
                .build();
    }

    private Adresse mottakerFrom(InfoRecord infoRecord) {
        return Adresse.builder()
                .withDigitalAdresse(OrganisasjonDigitalAdresse.builder()
                        .withOrgnr(infoRecord.getIdentifier())
                        .build())
                .withPostAdresse(getPostAdresse(infoRecord))
                .build();
    }

    private PostAdresse getPostAdresse(InfoRecord infoRecord) {
        PostAdresse.Builder<Void> builder = PostAdresse.builder()
                .withNavn(infoRecord.getOrganizationName());

        if (infoRecord.getPostadresse() != null) {
            builder.withAdresse1(infoRecord.getPostadresse().getAdresse())
                    .withPostnr(infoRecord.getPostadresse().getPostnummer())
                    .withPoststed(infoRecord.getPostadresse().getPoststed())
                    .withLand(infoRecord.getPostadresse().getLand());
        } else {
            builder.withPostnr("0192")
                    .withPoststed("Oslo")
                    .withLand("Norge");
        }

        return builder.build();
    }

    private List<Dokument> mapFrom(List<DokumentType> dokumentTypes, FileTypeHandlerFactory fileTypeHandlerFactory) {
        return dokumentTypes.stream()
                .map(d -> fileTypeHandlerFactory.createFileTypeHandler(d)
                        .map(Dokument.builder())
                        .withFilnavn(d.getVeFilnavn())
                        .withMimetype(d.getVeMimeType())
                        .build())
                .collect(Collectors.toList());
    }
}

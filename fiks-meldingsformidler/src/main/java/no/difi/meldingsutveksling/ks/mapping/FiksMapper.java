package no.difi.meldingsutveksling.ks.mapping;

import jakarta.activation.DataHandler;
import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.*;
import no.arkivverket.standarder.noark5.metadatakatalog.Korrespondanseparttype;
import no.arkivverket.standarder.noark5.metadatakatalog.TilknyttetRegistreringSom;
import no.difi.meldingsutveksling.DateTimeUtil;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateCMSDocument;
import no.difi.meldingsutveksling.domain.arkivmelding.JournalposttypeMapper;
import no.difi.meldingsutveksling.domain.arkivmelding.JournalstatusMapper;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.ks.svarut.*;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.move.common.io.ResourceDataSource;
import no.difi.move.common.io.pipe.Reject;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static no.difi.meldingsutveksling.NextMoveConsts.ARKIVMELDING_FILE;

@Slf4j
@Component
public class FiksMapper {

    private final IntegrasjonspunktProperties properties;
    private final ServiceRegistryLookup serviceRegistry;
    private final OptionalCryptoMessagePersister optionalCryptoMessagePersister;
    private final CreateCMSDocument createCMSDocument;
    private final ArkivmeldingUtil arkivmeldingUtil;
    private final Supplier<AlgorithmIdentifier> algorithmIdentifierSupplier;

    public FiksMapper(IntegrasjonspunktProperties properties,
                      ServiceRegistryLookup serviceRegistry,
                      OptionalCryptoMessagePersister optionalCryptoMessagePersister,
                      CreateCMSDocument createCMSDocument,
                      ArkivmeldingUtil arkivmeldingUtil,
                      Supplier<AlgorithmIdentifier> algorithmIdentifierSupplier) {
        this.properties = properties;
        this.serviceRegistry = serviceRegistry;
        this.optionalCryptoMessagePersister = optionalCryptoMessagePersister;
        this.createCMSDocument = createCMSDocument;
        this.algorithmIdentifierSupplier = algorithmIdentifierSupplier;
        this.arkivmeldingUtil = arkivmeldingUtil;
    }

    public SendForsendelseMedId mapFrom(NextMoveOutMessage message, X509Certificate certificate, Reject reject) throws NextMoveException {
        Optional<String> senderRef = SBDUtil.getOptionalSenderRef(message.getSbd());
        // Confirm that SenderRef is a valid UUID, else use messageId
        if (senderRef.isPresent()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                UUID.fromString(senderRef.get());
            } catch (IllegalArgumentException e) {
                senderRef = Optional.empty();
            }
        }
        String forsendelsesid = senderRef.orElse(message.getMessageId());
        return SendForsendelseMedId.builder()
            .withForsendelse(getForsendelse(message, certificate, reject))
            .withForsendelsesid(forsendelsesid)
            .build();
    }

    private Forsendelse getForsendelse(NextMoveOutMessage message, X509Certificate certificate, Reject reject) throws NextMoveException {
        Arkivmelding am = getArkivmelding(message);
        Saksmappe saksmappe = arkivmeldingUtil.getSaksmappe(am);
        Journalpost journalpost = arkivmeldingUtil.getJournalpost(am);

        Optional<String> receiverRef = SBDUtil.getOptionalReceiverRef(message.getSbd());
        if (receiverRef.isPresent()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                UUID.fromString(receiverRef.get());
            } catch (IllegalArgumentException e) {
                receiverRef = Optional.empty();
            }
        }

        return Forsendelse.builder()
            .withEksternref(message.getMessageId())
            .withForsendelseType(getForsendelseType(message))
            .withKunDigitalLevering(false)
            .withSvarPaForsendelse(receiverRef.orElse(null))
            .withTittel(journalpost.getOffentligTittel())
            .withKrevNiva4Innlogging(kreverNiva4Innlogging(message))
            .withKonteringskode(properties.getFiks().getUt().getKonteringsKode())
            .withKryptert(properties.getFiks().isKryptert())
            .withAvgivendeSystem("")
            .withPrintkonfigurasjon(getPrintkonfigurasjon())
            .withMottaker(getMottaker(message))
            .withSvarSendesTil(getSvarSendesTil(message, journalpost))
            .withMetadataFraAvleverendeSystem(metaDataFrom(saksmappe, journalpost))
            .withDokumenter(mapArkivmeldingDokumenter(message, getDokumentbeskrivelser(journalpost), certificate, reject))
            .withKunDigitalLevering(properties.getFiks().getUt().isKunDigitalLevering())
            .build();
    }

    private String getForsendelseType(NextMoveOutMessage message) {
        return getDpfSettings(message)
            .map(DpfSettings::getForsendelseType)
            .filter(StringUtils::hasText)
            .orElse(null);
    }

    private Optional<DpfSettings> getDpfSettings(NextMoveOutMessage message) {
        if (message.getBusinessMessage() instanceof ArkivmeldingMessage) {
            ArkivmeldingMessage arkivmeldingMessage = (ArkivmeldingMessage) message.getBusinessMessage();
            return Optional.ofNullable(arkivmeldingMessage.getDpf());
        }
        return Optional.empty();
    }

    private Printkonfigurasjon getPrintkonfigurasjon() {
        return Printkonfigurasjon.builder()
            .withTosidig(true)
            .withFargePrint(false)
            .withBrevtype(Brevtype.BPOST).build();
    }

    private Adresse getSvarSendesTil(NextMoveOutMessage message, Journalpost journalpost) {
        for (Korrespondansepart part : journalpost.getKorrespondansepart()) {
            if (part.getKorrespondanseparttype() == null) {
                log.warn("Invalid value for korrespondanseparttype element in Noark5 arkivmelding");
                journalpost.getKorrespondansepart().clear();
                break;
            }
        }
        return journalpost.getKorrespondansepart().stream()
            .filter(k -> k.getKorrespondanseparttype().equals(Korrespondanseparttype.AVSENDER))
            .map(a -> mottakerFrom(a, message.getSenderIdentifier()))
            .findFirst()
            .orElseGet(() -> mottakerFrom(serviceRegistry.getInfoRecord(message.getSenderIdentifier())));
    }

    private boolean kreverNiva4Innlogging(NextMoveOutMessage message) {
        Integer sikkerhetsnivaa = message.getBusinessMessage().getSikkerhetsnivaa();
        return sikkerhetsnivaa != null && sikkerhetsnivaa == 4;
    }

    private List<Dokumentbeskrivelse> getDokumentbeskrivelser(Journalpost journalpost) {
        return journalpost.getDokumentbeskrivelseAndDokumentobjekt().stream()
            .filter(Dokumentbeskrivelse.class::isInstance)
            .map(Dokumentbeskrivelse.class::cast)
            .sorted((o1, o2) -> {
                if (TilknyttetRegistreringSom.HOVEDDOKUMENT.equals(o1.getTilknyttetRegistreringSom())) {
                    return -1;
                } else if (TilknyttetRegistreringSom.HOVEDDOKUMENT.equals(o2.getTilknyttetRegistreringSom())) {
                    return 1;
                }
                return 0;
            })
            .collect(Collectors.toList());
    }

    private Adresse getMottaker(NextMoveOutMessage message) {
        final InfoRecord receiverInfo = serviceRegistry.getInfoRecord(message.getReceiverIdentifier());
        return mottakerFrom(receiverInfo);
    }

    private Arkivmelding getArkivmelding(NextMoveOutMessage message) throws NextMoveException {
        String identifier = getArkivmeldingIdentifier(message);
        try {
            Resource resource = optionalCryptoMessagePersister.read(message.getMessageId(), identifier);
            return arkivmeldingUtil.unmarshalArkivmelding(resource);
        } catch (JAXBException | IOException e) {
            throw new NextMoveRuntimeException("Failed to get Arkivmelding", e);
        }
    }

    private String getArkivmeldingIdentifier(NextMoveOutMessage message) throws NextMoveException {
        return message.getFiles().stream()
            .filter(f -> ARKIVMELDING_FILE.equals(f.getFilename()))
            .findAny()
            .map(BusinessMessageFile::getIdentifier)
            .orElseThrow(() -> new NextMoveException("No attachement \"%s\" found".formatted(ARKIVMELDING_FILE)));
    }

    private List<Dokument> mapArkivmeldingDokumenter(NextMoveOutMessage message, List<Dokumentbeskrivelse> docs, X509Certificate cert, Reject reject) {
        return docs.stream()
            .sorted((o1, o2) -> {
                if (TilknyttetRegistreringSom.HOVEDDOKUMENT.equals(o1.getTilknyttetRegistreringSom())) {
                    return -1;
                } else if (TilknyttetRegistreringSom.HOVEDDOKUMENT.equals(o2.getTilknyttetRegistreringSom())) {
                    return 1;
                }
                return 0;
            })
            .flatMap(p -> p.getDokumentobjekt().stream())
            .map(d -> getBusinessMessageFile(message, d.getReferanseDokumentfil()))
            .map(file -> getDocument(message.getMessageId(), file, cert, reject))
            .collect(Collectors.toList());
    }

    private BusinessMessageFile getBusinessMessageFile(NextMoveOutMessage message, String referanseDokumentfil) {
        return message.getFiles().stream()
            .filter(bmf -> bmf.getFilename().equals(referanseDokumentfil))
            .findFirst()
            .orElseThrow(() -> new NextMoveRuntimeException(
                "File '%s' referenced in '%s' not found".formatted(referanseDokumentfil, message.getMessageId())));
    }

    private Dokument getDocument(String messageId, BusinessMessageFile file, X509Certificate cert, Reject reject) {
        Resource document = readDocument(messageId, file);
        Resource encryptedDocument = createCMSDocument.encrypt(CreateCMSDocument.Input.builder()
            .resource(document)
            .certificate(cert)
            .keyEncryptionScheme(algorithmIdentifierSupplier.get())
            .build(), reject);

        Dokument.Builder<Void> builder = Dokument.builder()
            .withData(new DataHandler(new ResourceDataSource(encryptedDocument)))
            .withFilnavn(file.getFilename())
            .withMimetype(file.getMimetype());

        String ext = Stream.of(file.getFilename().split("\\.")).reduce((a, b) -> b).orElse("pdf");
        if (ext.equalsIgnoreCase("pdf")) {
            builder.withEkskluderesFraPrint(false);
        } else if (properties.getFiks().getUt().getEkskluderesFraPrint().contains("*")) {
            builder.withEkskluderesFraPrint(true);
        } else if (properties.getFiks().getUt().getEkskluderesFraPrint().contains(ext)) {
            builder.withEkskluderesFraPrint(true);
        }

        return builder.build();
    }

    private Resource readDocument(String messageId, BusinessMessageFile file) {
        try {
            return optionalCryptoMessagePersister.read(messageId, file.getIdentifier());
        } catch (IOException e) {
            throw new NextMoveRuntimeException("Could not read file named '%s' for messageId='%s'".formatted(file.getIdentifier(), messageId), e);
        }
    }

    private NoarkMetadataFraAvleverendeSakssystem metaDataFrom(Saksmappe sm, Journalpost jp) {
        return NoarkMetadataFraAvleverendeSakssystem.builder()
            .withSakssekvensnummer(toInt(sm.getSakssekvensnummer()))
            .withSaksaar(toInt(sm.getSaksaar()))
            .withJournalaar(toInt(jp.getJournalaar()))
            .withJournalsekvensnummer(toInt(jp.getJournalsekvensnummer()))
            .withJournalpostnummer(toInt(jp.getJournalpostnummer()))
            .withJournalposttype(JournalposttypeMapper.getNoarkType(jp.getJournalposttype()))
            .withJournalstatus(JournalstatusMapper.getNoarkType(jp.getJournalstatus()))
            .withJournaldato(DateTimeUtil.atStartOfDay(jp.getJournaldato()))
            .withDokumentetsDato(DateTimeUtil.atStartOfDay(jp.getDokumentetsDato()))
            .withTittel(jp.getOffentligTittel())
            .withSaksbehandler(getSaksbehandler(jp).orElse(null))
            .withEkstraMetadata(getEntries(jp))
            .build();
    }

    private Optional<String> getSaksbehandler(Journalpost jp) {
        return jp.getKorrespondansepart().stream()
            .filter(k -> k.getKorrespondanseparttype().equals(Korrespondanseparttype.AVSENDER))
            .findFirst()
            .map(Korrespondansepart::getSaksbehandler);
    }

    private int toInt(BigInteger x) {
        return x == null ? 0 : x.intValueExact();
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

    private List<Entry> getEntries(Journalpost journalpost) {
        Object virksomhetsspesifikkeMetadata = journalpost.getVirksomhetsspesifikkeMetadata();

        if (virksomhetsspesifikkeMetadata instanceof Node node) {

            NodeList nodeList = node.getChildNodes();

            return IntStream.range(0, nodeList.getLength())
                .filter(i -> nodeList.item(i) instanceof Element)
                .filter(i -> validVirksomhetsspesifikkeMetadata(nodeList.item(i)))
                .mapToObj(i -> Entry.builder().withKey((nodeList.item(i).getNodeName())).withValue(nodeList.item(i).getTextContent()).build())
                .toList();
        }

        return new ArrayList<>();
    }

    private boolean validVirksomhetsspesifikkeMetadata(Node node) {
        if(node.getChildNodes().getLength() != 1){
            log.warn("Only simple sub elements of virksomhetsspesifikkeMetadata can be transferred to SvarUt");
            return false;
        }
        return true;
    }
}

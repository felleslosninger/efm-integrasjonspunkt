package no.difi.meldingsutveksling.ks.svarut.rest;

import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.arkivverket.standarder.noark5.arkivmelding.Dokumentbeskrivelse;
import no.arkivverket.standarder.noark5.arkivmelding.Journalpost;
import no.arkivverket.standarder.noark5.arkivmelding.Korrespondansepart;
import no.arkivverket.standarder.noark5.arkivmelding.Saksmappe;
import no.arkivverket.standarder.noark5.metadatakatalog.Korrespondanseparttype;
import no.arkivverket.standarder.noark5.metadatakatalog.TilknyttetRegistreringSom;
import no.difi.meldingsutveksling.DateTimeUtil;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.BusinessMessage;
import no.difi.meldingsutveksling.domain.arkivmelding.JournalposttypeMapper;
import no.difi.meldingsutveksling.domain.arkivmelding.JournalstatusMapper;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingMessage;
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile;
import no.difi.meldingsutveksling.nextmove.DpfSettings;
import no.difi.meldingsutveksling.nextmove.HasSikkerhetsNivaa;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.move.common.dokumentpakking.CreateCMSDocument;
import no.difi.move.common.io.pipe.Reject;
import no.ks.fiks.svarut.forsendelse.send.model.v3.Adresse;
import no.ks.fiks.svarut.forsendelse.send.model.v3.Metadata;
import no.ks.fiks.svarut.forsendelse.send.model.v3.Mottaker;
import no.ks.fiks.svarut.forsendelse.send.model.v3.NoarkMetadataFraAvleverendeSakssystem;
import no.ks.fiks.svarut.forsendelse.send.model.v3.OrganisasjonForsendelse;
import no.ks.fiks.svarut.forsendelse.send.model.v3.OrganisasjonForsendelseDokumenterInner;
import no.ks.fiks.svarut.forsendelse.send.model.v3.Sikkerhetsniva;
import no.ks.fiks.svarut.forsendelse.send.model.v3.SvarSendesTil;
import no.ks.fiks.svarut.forsendelse.send.model.v3.Utskriftskonfigurasjon;
import no.ks.svarut.klient.forsendelse.send.v3.builders.AdresseBuilder;
import no.ks.svarut.klient.forsendelse.send.v3.builders.NoarkMetadataFraAvleverendeSakssystemBuilder;
import no.ks.svarut.klient.forsendelse.send.v3.builders.OrganisasjonForsendelseBuilder;
import no.ks.svarut.klient.forsendelse.send.v3.builders.UtskriftskonfigurasjonBuilder;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.springframework.core.io.Resource;
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
class FiksRestMapper {

    private final IntegrasjonspunktProperties properties;
    private final ServiceRegistryLookup serviceRegistry;
    private final OptionalCryptoMessagePersister optionalCryptoMessagePersister;
    private final CreateCMSDocument createCMSDocument;
    private final ArkivmeldingUtil arkivmeldingUtil;
    private final Supplier<AlgorithmIdentifier> algorithmIdentifierSupplier;

    public FiksRestMapper(IntegrasjonspunktProperties properties,
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

    OrganisasjonForsendelse getOrganisasjonForsendelse(NextMoveOutMessage message) throws NextMoveException {
        Arkivmelding am = getArkivmelding(message);
        Saksmappe saksmappe = arkivmeldingUtil.getSaksmappe(am);
        Journalpost journalpost = arkivmeldingUtil.getJournalpost(am);

        UUID receiverRef = SBDUtil.getOptionalReceiverRef(message.getSbd()).map(this::toUUID).orElse(null);

        return new OrganisasjonForsendelseBuilder()
            .eksternReferanse(message.getMessageId())
            .forsendelsestype(getForsendelseType(message))
            .kunDigitalLevering(false)
            .svarPaForsendelse(receiverRef)
            .tittel(journalpost.getOffentligTittel())
            .sikkerhetsniva(getSikkerhetsniva(message))
            .konteringskode(properties.getFiks().getUt().getKonteringsKode())
            .kryptert(properties.getFiks().isKryptert())
            .utskriftskonfigurasjon(getUtskriftskonfigurasjon())
            .mottaker(getMottaker(message))
            .svarSendesTil(getSvarSendesTil(message, journalpost))
            .metadataFraAvleverendeSystem(metaDataFrom(saksmappe, journalpost))
            .dokumenter(mapArkivmeldingDokumenter(message, getDokumentbeskrivelser(journalpost)))
            .kunDigitalLevering(properties.getFiks().getUt().isKunDigitalLevering())
            .build();
    }

    private String getForsendelseType(NextMoveOutMessage message) {
        return getDpfSettings(message)
            .map(DpfSettings::getForsendelseType)
            .filter(StringUtils::hasText)
            .orElse(null);
    }

    private Optional<DpfSettings> getDpfSettings(NextMoveOutMessage message) {
        if (message.getBusinessMessage() instanceof ArkivmeldingMessage arkivmeldingMessage) {
            return Optional.ofNullable(arkivmeldingMessage.getDpf());
        }
        return Optional.empty();
    }

    private Utskriftskonfigurasjon getUtskriftskonfigurasjon() {
        return new UtskriftskonfigurasjonBuilder()
            .tosidig(true)
            .build();
    }

    private SvarSendesTil getSvarSendesTil(NextMoveOutMessage message, Journalpost journalpost) {
        for (Korrespondansepart part : journalpost.getKorrespondansepart()) {
            if (part.getKorrespondanseparttype() == null) {
                log.warn("Invalid value for korrespondanseparttype element in Noark5 arkivmelding");
                journalpost.getKorrespondansepart().clear();
                break;
            }
        }
        return journalpost.getKorrespondansepart().stream()
            .filter(k -> k.getKorrespondanseparttype().equals(Korrespondanseparttype.AVSENDER))
            .map(a -> svarSendesTil(a, message.getSenderIdentifier()))
            .findFirst()
            .orElseGet(() -> svarSendesTil(serviceRegistry.getInfoRecord(message.getSenderIdentifier())));
    }

    static Sikkerhetsniva getSikkerhetsniva(NextMoveOutMessage message) {
        BusinessMessage businessMessage = message.getBusinessMessage();
        if (businessMessage instanceof HasSikkerhetsNivaa<?> e) {
            return Integer.valueOf(4).equals(e.getSikkerhetsnivaa()) ? Sikkerhetsniva.HOYESTE : Sikkerhetsniva.BETYDELIG;
        }

        return Sikkerhetsniva.BETYDELIG;
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

    private Mottaker getMottaker(NextMoveOutMessage message) {
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

    private List<OrganisasjonForsendelseDokumenterInner> mapArkivmeldingDokumenter(NextMoveOutMessage message, List<Dokumentbeskrivelse> docs) {
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
            .map(this::getOrganisasjonForsendelseDokumenterInner)
            .toList();
    }

    private BusinessMessageFile getBusinessMessageFile(NextMoveOutMessage message, String referanseDokumentfil) {
        return message.getFiles().stream()
            .filter(bmf -> bmf.getFilename().equals(referanseDokumentfil))
            .findFirst()
            .orElseThrow(() -> new NextMoveRuntimeException(
                "File '%s' referenced in '%s' not found".formatted(referanseDokumentfil, message.getMessageId())));
    }

    private OrganisasjonForsendelseDokumenterInner getOrganisasjonForsendelseDokumenterInner(BusinessMessageFile file) {
        return new OrganisasjonForsendelseDokumenterInner(file.getFilename(), file.getMimetype(), null, null,
            skalEkskluderesFraPrint(file.getFilename()), null, null);
    }

    private Boolean skalEkskluderesFraPrint(String filename) {
        String ext = Stream.of(filename.split("\\.")).reduce((a, b) -> b).orElse("pdf");
        if (ext.equalsIgnoreCase("pdf")) {
            return false;
        } else if (properties.getFiks().getUt().getEkskluderesFraPrint().contains("*")) {
            return true;
        } else if (properties.getFiks().getUt().getEkskluderesFraPrint().contains(ext)) {
            return true;
        }

        return null;
    }

    Resource getEncryptedDocument(String messageId, BusinessMessageFile file, X509Certificate cert, Reject reject) {
        Resource document = readDocument(messageId, file);
        return createCMSDocument.encrypt(CreateCMSDocument.Input.builder()
            .resource(document)
            .certificate(cert)
            .keyEncryptionScheme(algorithmIdentifierSupplier.get())
            .build(), reject);
    }

    private Resource readDocument(String messageId, BusinessMessageFile file) {
        try {
            return optionalCryptoMessagePersister.read(messageId, file.getIdentifier());
        } catch (IOException e) {
            throw new NextMoveRuntimeException("Could not read file named '%s' for messageId='%s'".formatted(file.getIdentifier(), messageId), e);
        }
    }

    private NoarkMetadataFraAvleverendeSakssystem metaDataFrom(Saksmappe sm, Journalpost jp) {
        return new NoarkMetadataFraAvleverendeSakssystemBuilder()
            .sakssekvensnummer(toLong(sm.getSakssekvensnummer()))
            .saksaar(toInt(sm.getSaksaar()))
            .journalaar(toInt(jp.getJournalaar()))
            .journalsekvensnummer(toLong(jp.getJournalsekvensnummer()))
            .journalpostnummer(toLong(jp.getJournalpostnummer()))
            .journalposttype(JournalposttypeMapper.getNoarkType(jp.getJournalposttype()))
            .journalstatus(JournalstatusMapper.getNoarkType(jp.getJournalstatus()))
            .journaldato(DateTimeUtil.toOffsetDateTime(DateTimeUtil.atStartOfDay(jp.getJournaldato())))
            .dokumentetsDato(DateTimeUtil.toOffsetDateTime(DateTimeUtil.atStartOfDay(jp.getDokumentetsDato())))
            .tittel(jp.getOffentligTittel())
            .saksbehandler(getSaksbehandler(jp).orElse(null))
            .ekstraMetadata(getMetadataList(jp))
            .build();
    }

    private Optional<String> getSaksbehandler(Journalpost jp) {
        return jp.getKorrespondansepart().stream()
            .filter(k -> k.getKorrespondanseparttype().equals(Korrespondanseparttype.AVSENDER))
            .findFirst()
            .map(Korrespondansepart::getSaksbehandler);
    }

    public UUID toUUID(String p) {
        try {
            return UUID.fromString(p);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Integer toInt(BigInteger x) {
        return x == null ? null : x.intValueExact();
    }

    private Long toLong(BigInteger x) {
        return x == null ? null : x.longValueExact();
    }

    private SvarSendesTil svarSendesTil(Korrespondansepart kp, String orgnr) {
        return new SvarSendesTil(kp.getKorrespondansepartNavn(), SvarSendesTil.Type.ORGANISASJON, orgnr, new AdresseBuilder()
            .adresselinje1(String.join(" ", kp.getPostadresse()))
            .postnummer(kp.getPostnummer())
            .poststed(kp.getPoststed())
            .build()
        );
    }

    private SvarSendesTil svarSendesTil(InfoRecord infoRecord) {
        return new SvarSendesTil(infoRecord.getOrganizationName(), SvarSendesTil.Type.ORGANISASJON, infoRecord.getIdentifier(), getPostAdresse(infoRecord));
    }

    private Mottaker mottakerFrom(InfoRecord infoRecord) {
        return new Mottaker(infoRecord.getOrganizationName(), infoRecord.getIdentifier(), getPostAdresse(infoRecord));
    }

    private Adresse getPostAdresse(InfoRecord infoRecord) {
        AdresseBuilder builder = new AdresseBuilder();

        if (infoRecord.getPostadresse() != null) {
            builder.adresselinje1(infoRecord.getPostadresse().getAdresse())
                .postnummer(infoRecord.getPostadresse().getPostnummer())
                .poststed(infoRecord.getPostadresse().getPoststed());
        } else {
            builder.poststed("0192")
                .postnummer("Oslo");
        }

        return builder.build();
    }

    private List<Metadata> getMetadataList(Journalpost journalpost) {
        Object virksomhetsspesifikkeMetadata = journalpost.getVirksomhetsspesifikkeMetadata();

        if (virksomhetsspesifikkeMetadata instanceof Node node) {

            NodeList nodeList = node.getChildNodes();

            return IntStream.range(0, nodeList.getLength())
                .filter(i -> nodeList.item(i) instanceof Element)
                .filter(i -> validVirksomhetsspesifikkeMetadata(nodeList.item(i)))
                .mapToObj(nodeList::item)
                .map(p -> new Metadata(p.getNodeName(), p.getTextContent()))
                .toList();
        }

        return new ArrayList<>();
    }

    private boolean validVirksomhetsspesifikkeMetadata(Node node) {
        if (node.getChildNodes().getLength() != 1) {
            log.warn("Only simple sub elements of virksomhetsspesifikkeMetadata can be transferred to SvarUt");
            return false;
        }
        return true;
    }
}

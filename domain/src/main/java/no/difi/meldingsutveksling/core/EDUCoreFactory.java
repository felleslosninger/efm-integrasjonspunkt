package no.difi.meldingsutveksling.core;

import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.*;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.arkivmelding.AvskrivningsmaateMapper;
import no.difi.meldingsutveksling.domain.arkivmelding.JournalposttypeMapper;
import no.difi.meldingsutveksling.domain.arkivmelding.TilknyttetRegistreringSomMapper;
import no.difi.meldingsutveksling.domain.arkivmelding.VariantformatMapper;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.noarkexchange.PayloadException;
import no.difi.meldingsutveksling.noarkexchange.PayloadUtil;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.EnvelopeType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.*;
import no.difi.meldingsutveksling.noarkexchange.schema.core.ObjectFactory;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Optional.ofNullable;
import static no.difi.meldingsutveksling.MimeTypeExtensionMapper.getMimetype;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPF;

@Slf4j
@Component
public class EDUCoreFactory {

    private ServiceRegistryLookup serviceRegistryLookup;

    public EDUCoreFactory(ServiceRegistryLookup serviceRegistryLookup) {
        this.serviceRegistryLookup = serviceRegistryLookup;
    }

    public EDUCore create(PutMessageRequestType putMessageRequestType, String senderOrgNr) {
        PutMessageRequestWrapper requestWrapper = new PutMessageRequestWrapper(putMessageRequestType);
        EDUCore eduCore = createCommon(senderOrgNr, requestWrapper.getRecieverPartyNumber(),
                requestWrapper.getEnvelope().getSender().getRef(), requestWrapper.getEnvelope().getReceiver().getRef());

        eduCore.setPayload(putMessageRequestType.getPayload());
        eduCore.setId(requestWrapper.getConversationId());
        if (PayloadUtil.isAppReceipt(putMessageRequestType.getPayload())) {
            eduCore.setMessageType(EDUCore.MessageType.APPRECEIPT);
        } else {
            eduCore.setMessageType(EDUCore.MessageType.EDU);
            String saIdXpath = "Melding/noarksak/saId";
            String jpostnrXpath = "Melding/journpost/jpJpostnr";
            String saId = "";
            String jpJpostnr = "";
            try {
                saId = PayloadUtil.queryPayload(eduCore.getPayload(), saIdXpath);
                jpJpostnr = PayloadUtil.queryPayload(eduCore.getPayload(), jpostnrXpath);
            } catch (PayloadException e) {
                log.error("Failed reading saId and/or jpJpostnr from payload", e);
            }

            if (!isNullOrEmpty(saId) && !isNullOrEmpty(jpJpostnr)) {
                eduCore.setMessageReference(String.format("%s-%s", saId, jpJpostnr));
            }
        }

        return eduCore;
    }

    public EDUCore create(AppReceiptType appReceiptType, String conversationId, String senderOrgnr, String receiverOrgnr) {
        EDUCore eduCore = createCommon(senderOrgnr, receiverOrgnr, null, null);
        eduCore.setId(conversationId);
        eduCore.setPayload(EDUCoreConverter.appReceiptAsString(appReceiptType));
        eduCore.setMessageType(EDUCore.MessageType.APPRECEIPT);
        return eduCore;
    }

    public static PutMessageRequestType createPutMessageFromCore(EDUCore message) {
        no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory of = new no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory();

        AddressType receiverAddressType = of.createAddressType();
        receiverAddressType.setOrgnr(message.getReceiver().getIdentifier());
        receiverAddressType.setName(message.getReceiver().getName());
        receiverAddressType.setRef(message.getReceiver().getRef());

        AddressType senderAddressType = of.createAddressType();
        senderAddressType.setOrgnr(message.getSender().getIdentifier());
        senderAddressType.setName(message.getSender().getName());
        senderAddressType.setRef(message.getSender().getRef());

        EnvelopeType envelopeType = of.createEnvelopeType();
        envelopeType.setConversationId(message.getId());
        envelopeType.setContentNamespace("http://www.arkivverket.no/Noark4-1-WS-WD/types");
        envelopeType.setReceiver(receiverAddressType);
        envelopeType.setSender(senderAddressType);

        PutMessageRequestType putMessageRequestType = of.createPutMessageRequestType();
        putMessageRequestType.setEnvelope(envelopeType);
        putMessageRequestType.setPayload(message.getPayload());

        return putMessageRequestType;
    }

    public EDUCore create(StandardBusinessDocument sbd, Arkivmelding am, byte[] asic) {
        String senderRef = sbd.findScope(ScopeType.SENDER_REF).map(Scope::getIdentifier).orElse(null);
        String receiverRef = sbd.findScope(ScopeType.RECEIVER_REF).map(Scope::getIdentifier).orElse(null);
        EDUCore eduCore = createCommon(sbd.getSenderIdentifier(), sbd.getReceiverIdentifier(), senderRef, receiverRef);
        eduCore.setId(sbd.getConversationId());
        eduCore.setMessageType(EDUCore.MessageType.EDU);
        eduCore.setMessageReference(sbd.getConversationId());

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

        eduCore.setPayload(EDUCoreConverter.meldingTypeAsString(meldingType));

        return eduCore;
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

    public byte[] getFileFromAsic(String fileName, byte[] bytes) throws IOException {
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


    private EDUCore createCommon(String senderOrgNr, String receiverOrgNr, String senderRef, String receiverRef) {

        InfoRecord senderInfo = serviceRegistryLookup.getInfoRecord(senderOrgNr);
        InfoRecord receiverInfo = serviceRegistryLookup.getInfoRecord(receiverOrgNr);

        EDUCore eduCore = new EDUCore();
        ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecord(receiverOrgNr);
        eduCore.setServiceIdentifier(serviceRecord.getServiceIdentifier());

        eduCore.setSender(Sender.of(senderInfo.getIdentifier(), senderInfo.getOrganizationName(), serviceRecord.getServiceIdentifier() == DPF ? senderRef : null));
        eduCore.setReceiver(Receiver.of(receiverInfo.getIdentifier(), receiverInfo.getOrganizationName(), serviceRecord.getServiceIdentifier() == DPF ? receiverRef : null));


        return eduCore;
    }

}

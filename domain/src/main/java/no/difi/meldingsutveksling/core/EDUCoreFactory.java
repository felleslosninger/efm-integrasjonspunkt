package no.difi.meldingsutveksling.core;

import com.google.common.collect.Lists;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.arkivverket.standarder.noark5.arkivmelding.Dokumentbeskrivelse;
import no.arkivverket.standarder.noark5.arkivmelding.Dokumentobjekt;
import no.arkivverket.standarder.noark5.arkivmelding.Registrering;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.mxa.schema.domain.Message;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import no.difi.meldingsutveksling.noarkexchange.PayloadUtil;
import no.difi.meldingsutveksling.noarkexchange.PutMessageMarker;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.EnvelopeType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.*;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.apache.commons.io.IOUtils;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static no.difi.meldingsutveksling.noarkexchange.PayloadUtil.unmarshallPayload;

public class EDUCoreFactory {

    private ServiceRegistryLookup serviceRegistryLookup;

    public EDUCoreFactory(ServiceRegistryLookup serviceRegistryLookup) {
        this.serviceRegistryLookup = serviceRegistryLookup;
    }

    public EDUCore create(PutMessageRequestType putMessageRequestType, String senderOrgNr) {
        PutMessageRequestWrapper requestWrapper = new PutMessageRequestWrapper(putMessageRequestType);
        EDUCore eduCore = createCommon(senderOrgNr, requestWrapper.getRecieverPartyNumber());

        try {
            eduCore.setPayload(unmarshallPayload(putMessageRequestType.getPayload()));
        } catch (JAXBException e) {
            Audit.error("Payload unmarshalling failed. Request causing error: {}",
                    PutMessageMarker.markerFrom(new PutMessageRequestWrapper(putMessageRequestType)), e);
            throw new MeldingsUtvekslingRuntimeException(e);
        }

        eduCore.setId(requestWrapper.getConversationId());
        if (PayloadUtil.isAppReceipt(putMessageRequestType.getPayload())) {
            eduCore.setMessageType(EDUCore.MessageType.APPRECEIPT);
        } else {
            eduCore.setMessageType(EDUCore.MessageType.EDU);
            Optional<String> saId = Optional.of(eduCore)
                    .map(EDUCore::getPayloadAsMeldingType)
                    .map(MeldingType::getNoarksak)
                    .map(NoarksakType::getSaId);
            Optional<String> jpostnr = Optional.of(eduCore)
                    .map(EDUCore::getPayloadAsMeldingType)
                    .map(MeldingType::getJournpost)
                    .map(JournpostType::getJpJpostnr);
            if (saId.isPresent() && jpostnr.isPresent()) {
                eduCore.setMessageReference(String.format("%s-%s", saId.get(), jpostnr.get()));
            }
        }


        return eduCore;
    }

    public static PutMessageRequestType createPutMessageFromCore(EDUCore message) {
        no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory of = new no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory();

        AddressType receiverAddressType = of.createAddressType();
        receiverAddressType.setOrgnr(message.getReceiver().getIdentifier());
        receiverAddressType.setName(message.getReceiver().getName());

        AddressType senderAddressType = of.createAddressType();
        senderAddressType.setOrgnr(message.getSender().getIdentifier());
        senderAddressType.setName(message.getSender().getName());

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

    public EDUCore create(Message message, String senderOrgNr) {
        EDUCore eduCore = createCommon(senderOrgNr, message.getParticipantId());

        // IdProc is regarded as message id for MXA, but UUID is needed by e.g. DPI.
        String genId = UUID.randomUUID().toString();
        eduCore.setId(genId);
        eduCore.setMessageReference(message.getMessageReference());
        eduCore.setMessageType(EDUCore.MessageType.EDU);

        ObjectFactory of = new ObjectFactory();

        JournpostType journpostType = of.createJournpostType();
        journpostType.setJpInnhold(message.getContent().getMessageHeader());
        journpostType.setJpOffinnhold(message.getContent().getMessageSummery());
        journpostType.setJpId(message.getMessageReference());

        message.getContent().getAttachments().getAttachment().forEach(a -> {
            DokumentType dokumentType = of.createDokumentType();
            dokumentType.setVeFilnavn(a.getFilename());
            dokumentType.setVeMimeType(a.getMimeType());
            dokumentType.setDbTittel(a.getName());
            FilType filType = of.createFilType();
            filType.setBase64(Base64.getDecoder().decode(a.getValue()));
            dokumentType.setFil(filType);

            journpostType.getDokument().add(dokumentType);
        });

        NoarksakType noarksakType = of.createNoarksakType();
        noarksakType.setSaOfftittel(message.getContent().getMessageHeader());

        MeldingType meldingType = of.createMeldingType();
        meldingType.setJournpost(journpostType);
        meldingType.setNoarksak(noarksakType);

        eduCore.setPayload(meldingType);

        return eduCore;
    }

    public EDUCore create(ConversationResource cr, Arkivmelding arkivmelding, byte[] asic) {
        EDUCore eduCore = createCommon(cr.getSenderId(), cr.getReceiverId());
        eduCore.setId(cr.getConversationId());
        eduCore.setMessageType(EDUCore.MessageType.EDU);
        eduCore.setMessageReference(cr.getConversationId());

        ObjectFactory of = new ObjectFactory();
        JournpostType journpostType = of.createJournpostType();

        List<Dokumentobjekt> docs = Lists.newArrayList();
        arkivmelding.getMappe().stream()
                .flatMap(m -> m.getBasisregistrering().stream())
                .forEach(r -> docs.addAll(dokObjFromReg(r)));
        arkivmelding.getRegistrering().forEach(r -> docs.addAll(dokObjFromReg(r)));

        docs.forEach(d -> {
            DokumentType dokumentType = of.createDokumentType();
            String filename = d.getReferanseDokumentfil();
            dokumentType.setVeFilnavn(filename);
            // TODO: tittel og mimetype
            FilType filType = of.createFilType();
            try {
                filType.setBase64(Base64.getDecoder().decode(getFileFromAsic(filename, asic)));
            } catch (IOException e) {
                throw new MeldingsUtvekslingRuntimeException(String.format("Error getting file %s from ASiC", filename), e);
            }
            dokumentType.setFil(filType);

            journpostType.getDokument().add(dokumentType);

        });

        NoarksakType noarksakType = of.createNoarksakType();
        // TODO: map arkivmelding til MeldingType

        MeldingType meldingType = of.createMeldingType();
        meldingType.setJournpost(journpostType);
        meldingType.setNoarksak(noarksakType);

        // Further usage expects payload to be marshalled
        EDUCoreConverter eduCoreConverter = new EDUCoreConverter();
        eduCore.setPayload(eduCoreConverter.meldingTypeAsString(meldingType));

        return eduCore;
    }

    private List<Dokumentobjekt> dokObjFromReg(Registrering r) {
        List<Dokumentobjekt> docs = Lists.newArrayList();
        r.getDokumentbeskrivelseAndDokumentobjekt().stream()
                .filter(o -> o instanceof Dokumentbeskrivelse)
                .flatMap(d -> ((Dokumentbeskrivelse)d).getDokumentobjekt().stream())
                .forEach(docs::add);
        r.getDokumentbeskrivelseAndDokumentobjekt().stream()
                .filter(o -> o instanceof Dokumentobjekt)
                .map(d -> (Dokumentobjekt)d)
                .forEach(docs::add);
        return docs;
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


    private EDUCore createCommon(String senderOrgNr, String receiverOrgNr) {

        InfoRecord senderInfo = serviceRegistryLookup.getInfoRecord(senderOrgNr);
        InfoRecord receiverInfo = serviceRegistryLookup.getInfoRecord(receiverOrgNr);

        EDUCore eduCore = new EDUCore();

        eduCore.setSender(createSender(senderInfo));
        eduCore.setReceiver(createReceiver(receiverInfo));

        ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecord(receiverOrgNr);
        eduCore.setServiceIdentifier(serviceRecord.getServiceIdentifier());

        return eduCore;
    }

    private Sender createSender(InfoRecord senderInfo) {
        Sender sender = new Sender();
        sender.setIdentifier(senderInfo.getIdentifier());
        sender.setName(senderInfo.getOrganizationName());
        return sender;
    }

    private Receiver createReceiver(InfoRecord receiverInfo) {
        Receiver receiver = new Receiver();
        receiver.setIdentifier(receiverInfo.getIdentifier());
        receiver.setName(receiverInfo.getOrganizationName());
        return receiver;
    }

}

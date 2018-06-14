package no.difi.meldingsutveksling.core;

import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingFactory;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import no.difi.meldingsutveksling.nextmove.ConversationResourceRepository;
import no.difi.meldingsutveksling.nextmove.DirectionalConversationResourceRepository;
import no.difi.meldingsutveksling.nextmove.DpaConversationResource;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.noarkexchange.PutMessageMarker;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
import no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.List;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPA;

/**
 *
 * @author Nikolai Luthman <nikolai dot luthman at inmeta dot no>
 */
@Component
public class PutMessageService {

    private final IntegrasjonspunktProperties properties;
    private final EDUCoreSender coreSender;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final InternalQueue queue;
    private final ConversationService conversationService;
    private final ArkivmeldingFactory arkivmeldingFactory;
    private final DirectionalConversationResourceRepository crRepo;
    private final MessagePersister persister;

    @Autowired
    public PutMessageService(
            IntegrasjonspunktProperties properties,
            EDUCoreSender coreSender,
            ServiceRegistryLookup serviceRegistryLookup,
            InternalQueue queue,
            ConversationService conversationService,
            ArkivmeldingFactory arkivmeldingFactory,
            ConversationResourceRepository crRepo, MessagePersister persister) {
        this.properties = properties;
        this.coreSender = coreSender;
        this.serviceRegistryLookup = serviceRegistryLookup;
        this.queue = queue;
        this.conversationService = conversationService;
        this.arkivmeldingFactory = arkivmeldingFactory;
        this.crRepo = new DirectionalConversationResourceRepository(crRepo, ConversationDirection.OUTGOING);
        this.persister = persister;
    }

    public PutMessageResponseType queueMessage(PutMessageRequestWrapper msg, ServiceRecord serviceRecord) {

        // Validate sender identifier
        if (!msg.hasSenderPartyNumber()) {
            Audit.error("Senders orgnr missing", PutMessageMarker.markerFrom(msg));
            throw new MeldingsUtvekslingRuntimeException("Missing senders orgnumber. Please configure orgnumber= in the integrasjonspunkt-local.properties");
        }
        // Validate receiver identifier
        if (!msg.hasRecieverPartyNumber()) {
            Audit.error("Receiver orgnr missing", PutMessageMarker.markerFrom(msg));
            throw new MeldingsUtvekslingRuntimeException("Missing receivers orgnumber.");
        }

        if (DPA.equals(serviceRecord.getServiceIdentifier())) {
            DpaConversationResource cr = DpaConversationResource.of(msg.getConversationId(),
                    msg.getSenderPartynumber(),
                    msg.getRecieverPartyNumber());

            Arkivmelding arkivmelding = arkivmeldingFactory.createArkivmeldingAndWriteFiles(msg);
            cr.setHasArkivmelding(true);

            List<String> files = ArkivmeldingUtil.getFilenames(arkivmelding);
            files.forEach(cr::addFileRef);

            try {
                byte[] amBytes = ArkivmeldingUtil.marshalArkivmelding(arkivmelding);
                persister.write(cr.getConversationId(), "arkivmelding.xml", amBytes);
                cr.addFileRef("arkivmelding.xml");
            } catch (JAXBException | IOException e) {
                throw new MeldingsUtvekslingRuntimeException("Could not marshal and save arkivmelding", e);
            }

            crRepo.save(cr);
            queue.enqueueNextmove(cr);
            return PutMessageResponseFactory.createOkResponse();
        }

        EDUCoreFactory eduCoreFactory = new EDUCoreFactory(serviceRegistryLookup);
        EDUCore message = eduCoreFactory.create(msg.getRequest(), msg.getSenderPartynumber());
        return queueMessage(message);
    }

    private PutMessageResponseType queueMessage(EDUCore message) {
        conversationService.registerConversation(message);
        if (properties.getFeature().isEnableQueue()) {
            queue.enqueueExternal(message);
            Audit.info("Message enqueued", EDUCoreMarker.markerFrom(message));
            return PutMessageResponseFactory.createOkResponse();
        } else {
            Audit.info("Queue is disabled", EDUCoreMarker.markerFrom(message));
            return coreSender.sendMessage(message);
        }
    }

}

package no.difi.meldingsutveksling.receipt.strategy;

import no.altinn.schemas.services.serviceengine.correspondence._2014._10.StatusChangeV2;
import no.altinn.schemas.services.serviceengine.correspondence._2014._10.StatusV2;
import no.altinn.services.serviceengine.correspondence._2009._10.GetCorrespondenceStatusDetailsV2;
import no.altinn.services.serviceengine.correspondence._2009._10.GetCorrespondenceStatusDetailsV2Response;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.difi.meldingsutveksling.ptv.CorrespondenceRequest;
import no.difi.meldingsutveksling.receipt.MessageReceipt;
import no.difi.meldingsutveksling.receipt.ReceiptStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static no.difi.meldingsutveksling.receipt.MessageReceiptMarker.markerFrom;

@Component
public class DpvReceiptStrategy implements ReceiptStrategy {

    @Autowired
    private IntegrasjonspunktProperties properties;

    @Override
    public boolean checkReceived(MessageReceipt receipt) {

        CorrespondenceAgencyConfiguration config = new CorrespondenceAgencyConfiguration.Builder()
                .withEndpointURL(properties.getAltinnPTV().getEndpointUrl())
                .withExternalServiceCode(properties.getAltinnPTV().getExternalServiceCode())
                .withExternalServiceEditionCode(properties.getAltinnPTV().getExternalServiceEditionCode())
                .withPassword(properties.getAltinnPTV().getPassword())
                .withSystemUserCode(properties.getAltinnPTV().getUsername())
                .build();
        final CorrespondenceAgencyClient client = new CorrespondenceAgencyClient(markerFrom(receipt), config);
        GetCorrespondenceStatusDetailsV2 receiptRequest = CorrespondenceAgencyMessageFactory.createReceiptRequest(receipt);
        final CorrespondenceRequest request = new CorrespondenceRequest.Builder().withUsername(config
                .getSystemUserCode()).withPassword(config.getPassword()).withPayload(receiptRequest).build();

        GetCorrespondenceStatusDetailsV2Response result = (GetCorrespondenceStatusDetailsV2Response) client
                .sendStatusRequest(request);

        // TODO: need to find a way to search for CorrespondenceIDs (in response( as ConversationID is not unqiue
        List<StatusV2> statusList = result.getGetCorrespondenceStatusDetailsV2Result().getValue().getStatusList().getValue().getStatusV2();
        StatusV2 firstStatus = statusList.stream().findFirst().get();
        List<StatusChangeV2> statusChanges = firstStatus.getStatusChanges().getValue().getStatusChangeV2();

        return statusChanges.stream().map(s -> s.getStatusType().value()).anyMatch(s -> "Created".equals(s));
    }
}

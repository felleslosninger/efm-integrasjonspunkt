package no.difi.meldingsutveksling.receipt.strategy;

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

import static no.difi.meldingsutveksling.receipt.MessageReceiptMarker.markerFrom;

@Component
public class DpvReceiptStrategy implements ReceiptStrategy {

    @Autowired
    private IntegrasjonspunktProperties properties;

    @Override
    public boolean checkCompleted(MessageReceipt receipt) {

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
        // check return status

        return false;
    }
}

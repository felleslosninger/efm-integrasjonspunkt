package no.difi.meldingsutveksling.dpi.json;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.status.ExternalReceipt;
import no.digdir.dpi.client.DpiClient;
import no.digdir.dpi.client.DpiException;
import no.digdir.dpi.client.domain.Shipment;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class JsonMeldingsformidlerClient implements MeldingsformidlerClient {

    private final DpiClient dpiClient;
    private final ShipmentFactory shipmentFactory;

    @Override
    public void sendMelding(MeldingsformidlerRequest request) throws MeldingsformidlerException {
        Shipment shipment = shipmentFactory.getShipment(request);

        try {
            dpiClient.send(shipment);
        } catch (DpiException e) {
            throw new MeldingsformidlerException("Send DPI message failed!", e);
        }
    }

    @Override
    public Flux<ExternalReceipt> sjekkEtterKvitteringer(String orgnr, String mpcId) {
        return null;
    }
}

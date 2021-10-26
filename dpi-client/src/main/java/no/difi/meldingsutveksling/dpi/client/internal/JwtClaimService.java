package no.difi.meldingsutveksling.dpi.client.internal;

import net.minidev.json.JSONObject;
import no.difi.meldingsutveksling.domain.sbdh.PartnerIdentification;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class JwtClaimService {

    public PartnerIdentification getAvsender(Map<String, Object> claims) {
        JSONObject object = (JSONObject) claims.get("consumer");
        return getPartnerIdentification(object);
    }

    public PartnerIdentification getDatabehandler(Map<String, Object> claims) {
        JSONObject object = (JSONObject) Optional.ofNullable(claims.get("supplier"))
                .orElseGet(() -> claims.get("consumer"));

        return getPartnerIdentification(object);
    }

    private PartnerIdentification getPartnerIdentification(JSONObject object) {
        return new PartnerIdentification()
                .setAuthority(object.getAsString("authority"))
                .setValue(object.getAsString("ID"));
    }
}

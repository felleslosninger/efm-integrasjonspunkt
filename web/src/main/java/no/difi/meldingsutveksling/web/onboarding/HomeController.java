package no.difi.meldingsutveksling.web.onboarding;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.web.FrontendFunctionality;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    @Inject
    FrontendFunctionality ff;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("message", "Integrasjonspunkt v4");
        model.addAttribute("token", "eyJhbGciOiJSUzI1NiIsImtpZCI6IjcxOUFGOTRFNDQ1MzE0Q0RDMjk1Rjk1MjUzODU4MDU0RjhCQ0FDODYiLCJ4NXQiOiJjWnI1VGtSVEZNM0NsZmxTVTRXQVZQaThySVkiLCJ0eXAiOiJKV1QifQ.eyJzY29wZSI6ImFsdGlubjpicm9rZXIud3JpdGUgYWx0aW5uOmJyb2tlci5yZWFkIiwidG9rZW5fdHlwZSI6IkJlYXJlciIsImV4cCI6MTc2MjQ1OTA0NywiaWF0IjoxNzYyNDU3MjQ3LCJjbGllbnRfaWQiOiJhNjNjYWM5MS0zMjEwLTRjMzUtYjk2MS01YzdiZjEyMjM0NWMiLCJjb25zdW1lciI6eyJhdXRob3JpdHkiOiJpc282NTIzLWFjdG9yaWQtdXBpcyIsIklEIjoiMDE5Mjo5OTE4MjU4MjcifSwidXJuOmFsdGlubjpvcmdOdW1iZXIiOiI5OTE4MjU4MjciLCJ1cm46YWx0aW5uOmF1dGhlbnRpY2F0ZW1ldGhvZCI6Im1hc2tpbnBvcnRlbiIsInVybjphbHRpbm46YXV0aGxldmVsIjozLCJpc3MiOiJodHRwczovL3BsYXRmb3JtLnR0MDIuYWx0aW5uLm5vL2F1dGhlbnRpY2F0aW9uL2FwaS92MS9vcGVuaWQvIiwianRpIjoiMzViNDRmMGItZWY2MC00YTJlLTk3OGUtNzZhZjJlMzZlYTU2IiwibmJmIjoxNzYyNDU3MjQ3fQ.lduC_6WFIZBBJO_kSaXoSK78Zh18Q0gaQsOt6_ffgN7bsnWPL0lKzPn5SIqzw2xHTT2D0WU_f8UOfmtUnf0uJ901wwd5QEb3ZxI6mp6dfbGDXbbflvdZSBVgs2RqsmzGxMw1Ok3VeCyT61_Q-NUN_h6i-yBSvg50VPGj91bcNlzYlXjeeh_aZcPGXp0Ks-tSXZEH3fYYUz6DuLGyS_Y0b8X6vBJI9vf51wjqoaM7BQGQAVDenBknlCTPtmw8SXozAPmT0Evf26gYUfAcIV6moDPV4C6mHgT65FNBh3uBLZSTa2faeLsILXjeRnL5DE3QQH-OdP0F3sMaSuLN-ua4ig");
        model.addAttribute("config", dpoProperties());
        model.addAttribute("messages", dpoMessages());
        model.addAttribute("channels", ff.getChannelsEnabled());
        return "index"; // corresponds to templates/index.html
    }

    record Property(String key, String value, String description) {}

    List<Property> dpoProperties() {
        return List.of(
                new Property("difi.move.org.number", "311780735", "Ditt organisasjonsnummer"),
                new Property("difi.move.org.keystore.path", "file:/Users/thorej/src/2023-cert-test-virks/eformidling-test-auth.jks", "Keystore med virksomhetssertifikat"),
                new Property("difi.move.org.keystore.alias", "digdir-test-eformidling", "Sertifikatets alias i keystore"),
                new Property("difi.move.org.keystore.password", "**********", "Passord på keystore"),
                new Property("difi.move.dpo.oidc.authenticationType", "JWK", "Maskinporten autentiserings type"),
                new Property("difi.move.dpo.oidc.jwk.path", "classpath:311780735-sterk-ulydig-hund-da.jwk", "JWK for autentisering i maskinporten"),
                new Property("difi.move.dpo.oidc.clientId", "b590f149-d0ba-4fca-b367-bccd9e444a00", "Maskinporten clientId"),
                new Property("difi.move.dpo.systemUser.orgId", "0192:311780735", "Din systembrukers orgId"),
                new Property("difi.move.dpo.systemUser.name", "311780735_systembruker_hund", "Din systembrukers navn"),
                new Property("difi.move.dpo.reportees[0].orgId", "0192:313711218", "Første på vegne av systembrukers orgId"),
                new Property("difi.move.dpo.reportees[0].name", "313711218_systembruker_ape", "Første på vegne av systembrukers navn")
        );
    }


    record DpoMessage(String transferId, String status, String sender, String sendersReference, String created, String expiration) {}

    List<DpoMessage> dpoMessages() {
        return List.of(
                new DpoMessage("3884c3e5-bc6e-4cc5-a541-af2119f60732", "Published", "0192:311780735", "ff13b6a0-197e-4935-affc-97937179b6af", "2025-11-03T08:47:06Z", "2025-12-03T08:47:06Z"),
                new DpoMessage("bb6259d5-1458-46ce-a0b9-450f9285f3d4", "Published", "0192:314240979", "3c1bfdc0-842b-483f-ab07-e447d09451a5", "2025-10-31T12:49:55Z", "2025-11-30T12:49:55Z")
        );
    }

}

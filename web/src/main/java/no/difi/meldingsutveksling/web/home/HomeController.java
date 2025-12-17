package no.difi.meldingsutveksling.web.home;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.web.FrontendFunctionality;
import no.difi.meldingsutveksling.web.FrontendState;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    @Inject
    FrontendFunctionality ff;

    @Inject
    FrontendState fs;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("ff", ff); // FrontendFunctionality is always available
        model.addAttribute("channels", ff.getChannelsEnabled());
        model.addAttribute("uptime", fs.getDurationSinceStartAsText());
        model.addAttribute("message", "Integrasjonspunkt v4");
        model.addAttribute("messages", dpoMessages());
        return "index"; // corresponds to templates/index.html
    }

    record DpoMessage(String transferId, String status, String sender, String sendersReference, String created, String expiration) {}

    List<DpoMessage> dpoMessages() {
        return List.of(
                new DpoMessage("3884c3e5-bc6e-4cc5-a541-af2119f60732", "Published", "0192:311780735", "ff13b6a0-197e-4935-affc-97937179b6af", "2025-11-03T08:47:06Z", "2025-12-03T08:47:06Z"),
                new DpoMessage("bb6259d5-1458-46ce-a0b9-450f9285f3d4", "Published", "0192:314240979", "3c1bfdc0-842b-483f-ab07-e447d09451a5", "2025-10-31T12:49:55Z", "2025-11-30T12:49:55Z")
        );
    }

}

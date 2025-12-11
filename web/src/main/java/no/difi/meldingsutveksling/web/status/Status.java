package no.difi.meldingsutveksling.web.status;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.web.FrontendFunctionality;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Random;

@Controller
public class Status {

    @Inject
    FrontendFunctionality ff;

    record RuntimeStatus(String service, String config, int errors){}

    @GetMapping("/status")
    public String showIndexPage(Model model) {
        var random = new Random();
        var stats = ff.getChannelsEnabled().stream().map(
            si -> new RuntimeStatus(si, si, random.nextInt(1000))
        ).toList();
        model.addAttribute("status", stats);
        return "status";
    }

}

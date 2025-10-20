package no.difi.meldingsutveksling.web;

import no.difi.meldingsutveksling.ServiceIdentifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;
import java.util.Random;

@Controller
public class Status {

    record RuntimeStatus(ServiceIdentifier service, String config, int errors){}

    @GetMapping("/status")
    public String showIndexPage(Model model) {
        var random = new Random();
        var stats = Arrays.stream(ServiceIdentifier.values()).map(
            si -> new RuntimeStatus(si, si.name(), random.nextInt(1000))
        ).toList();
        model.addAttribute("status", stats);
        return "status";
    }

}

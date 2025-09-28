package no.difi.meldingsutveksling.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Random;

@Controller
public class Status {

    record RuntimeStatus(boolean dpo, boolean dpv, boolean dpi){}

    @GetMapping("/status")
    public String showIndexPage(Model model) {
        var random = new Random();
        var randomStatus = new RuntimeStatus(random.nextBoolean(), random.nextBoolean(), random.nextBoolean());
        model.addAttribute("status", randomStatus);
        return "status";
    }

}

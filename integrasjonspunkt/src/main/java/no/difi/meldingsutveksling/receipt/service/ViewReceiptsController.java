package no.difi.meldingsutveksling.receipt.service;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewReceiptsController {

    @GetMapping("/viewreceipts")
    public String viewReceipts() {
        return "viewreceipts";
    }
}

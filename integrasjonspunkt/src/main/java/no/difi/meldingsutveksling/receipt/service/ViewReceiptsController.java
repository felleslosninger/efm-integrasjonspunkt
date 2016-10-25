package no.difi.meldingsutveksling.receipt.service;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class ViewReceiptsController {

    @RequestMapping(method = RequestMethod.GET, value = "/viewreceipts")
    public String viewReceipts() {
        return "viewreceipts";
    }
}

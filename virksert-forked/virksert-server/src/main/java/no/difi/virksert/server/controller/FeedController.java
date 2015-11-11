package no.difi.virksert.server.controller;

import no.difi.virksert.server.logic.RegistrationService;
import no.difi.xsd.virksert.model._1.Certificate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping(value = "/feed")
public class FeedController {

    @Autowired
    private RegistrationService registrationService;

    @ResponseBody
    @RequestMapping(value = "/expiring.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Certificate> listExpiringJson() {
        return registrationService.listExpiredAsCertificates();
    }

    @ResponseBody
    @RequestMapping(value = "/revoked.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Certificate> listRevokedJson() {
        return registrationService.listRevokedAsCertificates();
    }

    @ResponseBody
    @RequestMapping(value = "/updated.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Certificate> listUpdatedJson() {
        return registrationService.listUpdatedAsCertificates();
    }

}

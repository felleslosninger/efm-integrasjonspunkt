package no.difi.virksert.server.controller;

import no.difi.certvalidator.Validator;
import no.difi.virksert.server.logic.RegistrationService;
import no.difi.virksert.server.model.Registration;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.security.cert.X509Certificate;

@Controller
@RequestMapping("/cert")
public class CertificateController {

    private static Logger logger = LoggerFactory.getLogger(CertificateController.class);

    @Autowired
    private RegistrationService registrationService;

    @RequestMapping
    public String front(@RequestParam(value = "identifier", required = false) String identifier) {
        if (identifier != null)
            return String.format("redirect:/cert/%s", identifier);

        return "cert";
    }

    @RequestMapping("/{identifier}")
    public String view(@PathVariable("identifier") String identifier, ModelMap modelMap) throws Exception{
        Registration registration = registrationService.findByIdentifier(identifier);

        if (registration == null) {
            return "certificate_notfound";
        } else if (registration.getRevoked() != 0) {
            return "certificate_revoked";
        } else if (registration.getExpiration() < System.currentTimeMillis()) {
            return "certificate_expired";
        }

        X509Certificate certificate = Validator.getCertificate(registration.getCertificate());

        modelMap.put("certificate", certificate);
        modelMap.put("identifier", identifier);
        modelMap.put("registration", registration);
        return "certificate_view";
    }

    @RequestMapping({"/{identifier}.cer", "/{identifier}.crt"})
    public void cert(@PathVariable("identifier") String identifier, HttpServletResponse response) throws Exception {
        Registration registration = registrationService.findByIdentifier(identifier);
        logger.debug("{}", registration);

        if (registration == null) {
            response.setStatus(404);
            response.getWriter().println("Not found.");
            return;
        } else if (registration.getRevoked() != 0) {
            response.setStatus(410);
            response.getWriter().println("Revoked.");
            return;
        } else if (registration.getExpiration() < System.currentTimeMillis()) {
            response.setStatus(410);
            response.getWriter().println("Expired.");
            return;
        }

        response.setContentType("application/pkix-cert");
        response.addHeader("Content-Disposition", "inline; filename=\"" + registration.getIdentifier() + ".cer\"");
        response.getWriter().println("-----BEGIN CERTIFICATE-----");
        response.getWriter().println(Base64.encodeBase64String(registration.getCertificate()));
        response.getWriter().print("-----END CERTIFICATE-----");
    }
}

package no.difi.meldingsutveksling.eventlog;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 */
@RestController
@RequestMapping("/evenLog")
public class EventLogController {

    @RequestMapping("/hello")
    public String hello() {
        return "world";
    }

}

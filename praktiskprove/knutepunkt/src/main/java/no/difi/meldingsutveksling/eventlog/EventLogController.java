package no.difi.meldingsutveksling.eventlog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *
 */
@RestController
@RequestMapping("/eventLog")
public class EventLogController {

    @Autowired
    private EventLogDAO eventLogDAO;

    @RequestMapping(value = "/event", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<Event> getEventsSince(@RequestParam(value = "since") long since) {
        return eventLogDAO.getEventLog(since);
    }

    @RequestMapping(value = "/event", method = RequestMethod.PUT, consumes = {"application/json"})
    public void log(@RequestBody Event event) {
        eventLogDAO.insertEventLog(event);
    }

    public EventLogDAO getEventLogDAO() {
        return eventLogDAO;
    }

    public void setEventLogDAO(EventLogDAO eventLogDAO) {
        this.eventLogDAO = eventLogDAO;
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handle(HttpMessageNotReadableException e) {
        System.out.println(e);
    }

}

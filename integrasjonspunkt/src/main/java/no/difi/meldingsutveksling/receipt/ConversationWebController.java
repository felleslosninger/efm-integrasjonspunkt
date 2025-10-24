package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.status.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/conversations")
public class ConversationWebController {

    private final ConversationRepository conversationRepository;
    private final Logger log = LoggerFactory.getLogger(ConversationWebController.class);

    public ConversationWebController(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String home(
            Model model,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String direction,
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            @RequestParam(required = false) LocalDate created,
            @PageableDefault(sort = {"lastUpdate"}, direction = Sort.Direction.DESC, size = 20) Pageable pageable
    ) {
        var findAll = conversationRepository.findWithMessageStatuses(search, direction, created, pageable);
        model.addAttribute("search", search);
        model.addAttribute("created", created);
        model.addAttribute("direction", direction);
        model.addAttribute("page", findAll);

        Map<String, String> lastStatusMap = new HashMap<>();
        findAll.getContent().forEach(it -> {
            var st = it.getMessageStatuses().stream()
                    .max(Comparator.comparing(no.difi.meldingsutveksling.status.MessageStatus::getLastUpdate))
                    .map(no.difi.meldingsutveksling.status.MessageStatus::getStatus)
                    .orElse("none");
            lastStatusMap.put(it.getMessageId(), st);
        });
        model.addAttribute("statusMap", lastStatusMap);

        return "conversations/index";
    }

}

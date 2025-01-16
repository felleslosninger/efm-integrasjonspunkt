package no.difi.meldingsutveksling.receipt

import no.difi.meldingsutveksling.status.ConversationRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@RequestMapping("/conversations")
@Controller
open class ConversationWebController(
    private val conversationRepository: ConversationRepository
) {
    val log = LoggerFactory.getLogger(ConversationWebController::class.java)

    @GetMapping
    @Transactional(readOnly = true)
    open fun home(
        model: Model,
        @RequestParam(value = "search", required = false) search: String?,
        @RequestParam(value = "direction", required = false) direction: String?,
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @RequestParam(value = "created", required = false) created: LocalDate?,
        @PageableDefault(sort = ["lastUpdate"], direction = Sort.Direction.DESC, size = 20) pageable: Pageable
    ): String {
        val findAll = conversationRepository.findWithMessageStatuses(search, direction, created, pageable)
        model.addAttribute("search", search)
        model.addAttribute("created", created)
        model.addAttribute("direction", direction)
        model.addAttribute("page", findAll)

        val lastStatusMap = findAll.content.map {
            it.messageId to (it.messageStatuses.maxByOrNull { st -> st.lastUpdate }?.status ?: "none")
        }.toMap()
        model.addAttribute("statusMap", lastStatusMap)

        return "conversations/index"
    }
}
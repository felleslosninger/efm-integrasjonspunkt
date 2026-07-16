package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.clock.ClockConfig;
import no.difi.meldingsutveksling.nextmove.JacksonMockitoConfig;
import no.difi.meldingsutveksling.oauth2.Oauth2ClientSecurityConfig;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.ConversationQueryInput;
import no.difi.meldingsutveksling.status.ConversationRepository;
import no.difi.meldingsutveksling.status.service.ConversationController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that the UrlHandlerFilter registered by {@link MvcConfiguration} preserves the
 * deprecated trailing slash matching when difi.move.feature.allowDeprecatedTrailingSlash is
 * enabled. See {@link MvcConfigurationTrailingSlashDisabledTest} for the default behaviour.
 */
@Import({ClockConfig.class, JacksonConfig.class, JacksonMockitoConfig.class, MvcConfiguration.class})
@WebMvcTest({Oauth2ClientSecurityConfig.class, ConversationController.class})
@ActiveProfiles("test")
@TestPropertySource(properties = "difi.move.feature.allowDeprecatedTrailingSlash=true")
public class MvcConfigurationTrailingSlashEnabledTest {

    @Autowired private MockMvc mvc;

    @MockitoBean private ConversationRepository conversationRepository;

    @Test
    public void requestWithTrailingSlashMatchesHandler() throws Exception {
        givenConversations();

        mvc.perform(
                get("/api/conversations/")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "testpassword"))
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());
    }

    @Test
    public void requestWithoutTrailingSlashStillMatchesHandler() throws Exception {
        givenConversations();

        mvc.perform(
                get("/api/conversations")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "testpassword"))
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());
    }

    private void givenConversations() {
        given(conversationRepository.findWithMessageStatuses(any(ConversationQueryInput.class), any(Pageable.class)))
                .willAnswer(invocation -> new PageImpl<Conversation>(List.of(), invocation.getArgument(1), 0));
    }
}

package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.difi.meldingsutveksling.clock.FixedClockConfig;
import no.difi.meldingsutveksling.config.JacksonConfig;
import no.difi.meldingsutveksling.domain.webhooks.Subscription;
import no.difi.meldingsutveksling.validation.group.ValidationGroups;
import no.difi.meldingsutveksling.webhooks.filter.WebhookFilterParser;
import no.difi.meldingsutveksling.webhooks.subscription.SubscriptionController;
import no.difi.meldingsutveksling.webhooks.subscription.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static no.difi.meldingsutveksling.nextmove.RestDocumentationCommon.*;
import static no.difi.meldingsutveksling.nextmove.SubscriptionTestData.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@Import({FixedClockConfig.class, JacksonConfig.class, JacksonMockitoConfig.class})
@WebMvcTest(SubscriptionController.class)
@AutoConfigureMoveRestDocs
@ActiveProfiles("test")
@ComponentScan(basePackageClasses = WebhookFilterParser.class)
public class SubscriptionControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private SubscriptionService subscriptionService;

    @Test
    public void listSubscriptions() throws Exception {
        given(subscriptionService.listSubscriptions(any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<Subscription> content = Arrays.asList(incomingMessages(), failedMessages());
                    return new PageImpl<>(content, invocation.getArgument(0), content.size());
                });

        mvc.perform(
                get("/api/subscriptions")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("subscriptions/list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        requestParameters(getPagingParameterDescriptors()),
                        responseFields()
                                .and(subscriptionDescriptors("content[].", null))
                                .and(pageDescriptors())
                                .andWithPrefix("pageable.", pageableDescriptors())
                        )
                );

        verify(subscriptionService).listSubscriptions(any(Pageable.class));
    }

    @Test
    public void listPaging() throws Exception {
        given(subscriptionService.listSubscriptions(any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<Subscription> content = Collections.singletonList(failedMessages());
                    return new PageImpl<>(content, invocation.getArgument(0), content.size());
                });

        mvc.perform(
                get("/api/subscriptions")
                        .param("page", "1")
                        .param("size", "1")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("subscriptions/list/paging"));

        verify(subscriptionService).listSubscriptions(any(Pageable.class));
    }

    @Test
    public void getSubscription() throws Exception {
        Subscription subscription = incomingMessages();
        given(subscriptionService.getSubscription(any())).willReturn(subscription);

        mvc.perform(
                get("/api/subscriptions/{id}", subscription.getId())
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("subscriptions/get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        pathParameters(
                                parameterWithName("id")
                                        .description("The id of the subscription to retrieve")
                        ),
                        responseFields(subscriptionDescriptors("", null))
                ));

        verify(subscriptionService).getSubscription(eq(subscription.getId()));
    }

    @Test
    public void createSubscription() throws Exception {
        Subscription input = incomingMessagesInput();
        Subscription subscription = incomingMessages();
        given(subscriptionService.createSubscription(any())).willReturn(subscription);

        mvc.perform(
                post("/api/subscriptions")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input))

        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("subscriptions/create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        requestFields(
                                subscriptionInputDescriptors("", ValidationGroups.Create.class)
                        ),
                        responseFields(
                                subscriptionDescriptors("", ValidationGroups.Create.class)
                        )
                ));

        verify(subscriptionService).createSubscription(any());
    }

    @Test
    public void updateSubscription() throws Exception {
        Subscription input = incomingMessagesInput();
        Subscription subscription = incomingMessages();

        mvc.perform(
                put("/api/subscriptions/{id}", subscription.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input))
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("subscriptions/update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        pathParameters(
                                parameterWithName("id")
                                        .description("The id of the subscription to update.")
                        ),
                        requestFields(
                                subscriptionInputDescriptors("", ValidationGroups.Update.class)
                        )
                ));

        verify(subscriptionService).updateSubscription(anyLong(), any());
    }

    @Test
    public void deleteSubscription() throws Exception {
        Subscription subscription = incomingMessages();

        mvc.perform(
                delete("/api/subscriptions/{id}", subscription.getId())
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("subscriptions/delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        pathParameters(
                                parameterWithName("id")
                                        .description("The id of the subscription to update.")
                        )
                ));

        verify(subscriptionService).deleteSubscription(anyLong());
    }

    @Test
    public void deleteAllSubscriptions() throws Exception {
        mvc.perform(
                delete("/api/subscriptions")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("subscriptions/delete-all",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        )
                ));

        verify(subscriptionService).deleteAll();
    }
}
package no.difi.meldingsutveksling.receipt.service;

import no.difi.meldingsutveksling.clock.FixedClockConfig;
import no.difi.meldingsutveksling.config.JacksonConfig;
import no.difi.meldingsutveksling.nextmove.v2.CapabilitiesController;
import no.difi.meldingsutveksling.nextmove.v2.CapabilitiesFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static no.difi.meldingsutveksling.receipt.service.CapabilityTestData.*;
import static no.difi.meldingsutveksling.receipt.service.RestDocumentationCommon.capabilitiesDescriptors;
import static no.difi.meldingsutveksling.receipt.service.RestDocumentationCommon.getDefaultHeaderDescriptors;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@Import({FixedClockConfig.class, JacksonConfig.class, JacksonMockitoConfig.class})
@WebMvcTest(CapabilitiesController.class)
@AutoConfigureMoveRestDocs
@TestPropertySource("classpath:/config/application-test.properties")
@ActiveProfiles("test")
public class CapabilitiesControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CapabilitiesFactory capabilitiesFactory;

    @Test
    public void getCapabilitiesDPI() throws Exception {
        given(capabilitiesFactory.getCapabilities(anyString(), isNull())).willReturn(capabilitiesDPI());

        mvc.perform(
                get("/api/capabilities/{receiverIdentifier}", "01017012345")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("capabilities/dpi",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        pathParameters(
                                parameterWithName("receiverIdentifier").optional().description("The receiverIdentifier to get the capabilities for.")
                        ),
                        requestParameters(
                                parameterWithName("securityLevel").optional().description("An optional security level. Is an integer like 1, 2, 3 or 4.")
                        ),
                        responseFields(capabilitiesDescriptors())
                        )
                );

        verify(capabilitiesFactory).getCapabilities(eq("01017012345"), isNull());
    }

    @Test
    public void getCapabilitiesDPIWithSecurityLevel() throws Exception {
        given(capabilitiesFactory.getCapabilities(anyString(), any(Integer.class))).willReturn(capabilitiesDPI());

        mvc.perform(
                get("/api/capabilities/{receiverIdentifier}", "01017012345")
                        .param("securityLevel", "4")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("capabilities/dpi/security-level",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        pathParameters(
                                parameterWithName("receiverIdentifier").optional().description("The receiverIdentifier to get the capabilities for - Should not include ICD.")
                        ),
                        requestParameters(
                                parameterWithName("securityLevel").optional().description("An optional security level. Is an integer like 1, 2, 3 or 4.")
                        ),
                        responseFields(capabilitiesDescriptors())
                        )
                );

        verify(capabilitiesFactory).getCapabilities("01017012345", 4);
    }

    @Test
    public void getCapabilitiesDPO() throws Exception {
        given(capabilitiesFactory.getCapabilities(anyString(), isNull())).willReturn(capabilitiesDPO());

        mvc.perform(
                get("/api/capabilities/{receiverIdentifier}", "987654321")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("capabilities/dpo",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        pathParameters(
                                parameterWithName("receiverIdentifier").optional().description("The receiverIdentifier to get the capabilities for. Should not include ICD.")
                        ),
                        requestParameters(
                                parameterWithName("securityLevel").optional().description("An optional security level. Is an integer like 1, 2, 3 or 4.")
                        ),
                        responseFields(capabilitiesDescriptors())
                        )
                );

        verify(capabilitiesFactory).getCapabilities(eq("987654321"), isNull());
    }

    @Test
    public void getCapabilitiesDPOWithSecurityLevel() throws Exception {
        given(capabilitiesFactory.getCapabilities(anyString(), any(Integer.class))).willReturn(capabilitiesDPO());

        mvc.perform(
                get("/api/capabilities/{receiverIdentifier}", "987654321")
                        .param("securityLevel", "4")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("capabilities/dpo/security-level",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        pathParameters(
                                parameterWithName("receiverIdentifier").optional().description("The receiverIdentifier to get the capabilities for.")
                        ),
                        requestParameters(
                                parameterWithName("securityLevel").optional().description("An optional security level. Is an integer like 1, 2, 3 or 4.")
                        ),
                        responseFields(capabilitiesDescriptors())
                        )
                );

        verify(capabilitiesFactory).getCapabilities("987654321", 4);
    }
}

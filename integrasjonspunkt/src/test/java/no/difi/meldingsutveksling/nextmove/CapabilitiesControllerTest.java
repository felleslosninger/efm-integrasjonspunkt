package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.clock.FixedClockConfig;
import no.difi.meldingsutveksling.config.JacksonConfig;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.domain.PersonIdentifier;
import no.difi.meldingsutveksling.nextmove.v2.CapabilitiesController;
import no.difi.meldingsutveksling.nextmove.v2.CapabilitiesFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static no.difi.meldingsutveksling.nextmove.CapabilityTestData.capabilitiesDPI;
import static no.difi.meldingsutveksling.nextmove.CapabilityTestData.capabilitiesDPO;
import static no.difi.meldingsutveksling.nextmove.RestDocumentationCommon.capabilitiesDescriptors;
import static no.difi.meldingsutveksling.nextmove.RestDocumentationCommon.getDefaultHeaderDescriptors;
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

@ExtendWith(SpringExtension.class)
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
        given(capabilitiesFactory.getCapabilities(any(PartnerIdentifier.class), isNull(), isNull())).willReturn(capabilitiesDPI());

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

        verify(capabilitiesFactory).getCapabilities(eq(PersonIdentifier.parse("01017012345")), isNull(), isNull());
    }

    @Test
    public void getCapabilitiesDPIWithSecurityLevel() throws Exception {
        given(capabilitiesFactory.getCapabilities(any(PartnerIdentifier.class), any(Integer.class), isNull())).willReturn(capabilitiesDPI());

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

        verify(capabilitiesFactory).getCapabilities(eq(PersonIdentifier.parse("01017012345")), eq(4), isNull());
    }

    @Test
    public void getCapabilitiesDPIWithProcess() throws Exception {
        given(capabilitiesFactory.getCapabilities(any(PartnerIdentifier.class), isNull(), anyString())).willReturn(capabilitiesDPI());

        mvc.perform(
                get("/api/capabilities/{receiverIdentifier}", "01017012345")
                        .param("process", "admin-process")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("capabilities/dpi/process",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        pathParameters(
                                parameterWithName("receiverIdentifier").optional().description("The receiverIdentifier to get the capabilities for - Should not include ICD.")
                        ),
                        requestParameters(
                                parameterWithName("process").optional().description("An optional parameter to retrieve a specific process.")
                        ),
                        responseFields(capabilitiesDescriptors())
                        )
                );

        verify(capabilitiesFactory).getCapabilities(eq(PersonIdentifier.parse("01017012345")), isNull(), eq("admin-process"));
    }

    @Test
    public void getCapabilitiesDPO() throws Exception {
        given(capabilitiesFactory.getCapabilities(any(PartnerIdentifier.class), isNull(), isNull())).willReturn(capabilitiesDPO());

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

        verify(capabilitiesFactory).getCapabilities(eq(Iso6523.of(ICD.NO_ORG,"987654321")), isNull(), isNull());
    }

    @Test
    public void getCapabilitiesDPOWithSecurityLevel() throws Exception {
        given(capabilitiesFactory.getCapabilities(any(PartnerIdentifier.class), any(Integer.class), isNull())).willReturn(capabilitiesDPO());

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

        verify(capabilitiesFactory).getCapabilities(eq(Iso6523.of(ICD.NO_ORG,"987654321")), eq(4), isNull());
    }

    @Test
    public void getCapabilitiesDPOWithProcess() throws Exception {
        given(capabilitiesFactory.getCapabilities(any(PartnerIdentifier.class), isNull(), anyString())).willReturn(capabilitiesDPO());

        mvc.perform(
                get("/api/capabilities/{receiverIdentifier}", "987654321")
                        .param("process", "admin-process")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("capabilities/dpo/process",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        pathParameters(
                                parameterWithName("receiverIdentifier").optional().description("The receiverIdentifier to get the capabilities for.")
                        ),
                        requestParameters(
                                parameterWithName("process").optional().description("An optional parameter to retrieve a specific process.")
                        ),
                        responseFields(capabilitiesDescriptors())
                        )
                );

        verify(capabilitiesFactory).getCapabilities(eq(Iso6523.of(ICD.NO_ORG,"987654321")), isNull(), eq("admin-process"));
    }
}

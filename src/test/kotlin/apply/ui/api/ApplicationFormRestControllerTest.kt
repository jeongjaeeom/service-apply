package apply.ui.api

import apply.application.ApplicantAndFormResponse
import apply.application.ApplicantService
import apply.application.ApplicationFormResponse
import apply.application.ApplicationFormService
import apply.application.MyApplicationFormResponse
import apply.createApplicationForm
import apply.createApplicationForms
import apply.createUser
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation
import org.springframework.test.web.servlet.get

@WebMvcTest(
    controllers = [ApplicationFormRestController::class]
)
internal class ApplicationFormRestControllerTest : RestControllerTest() {
    @MockkBean
    private lateinit var applicationFormService: ApplicationFormService

    @MockkBean
    private lateinit var applicantService: ApplicantService

    private val applicationFormResponse = ApplicationFormResponse(createApplicationForm())
    private val myApplicationFormResponses = createApplicationForms().map(::MyApplicationFormResponse)
    private val userKeyword = "아마찌"
    private val applicantAndFormResponses = listOf(
        ApplicantAndFormResponse(createUser(name = "로키"), false, createApplicationForms()[0]),
        ApplicantAndFormResponse(createUser(name = userKeyword), false, createApplicationForms()[1])
    )
    private val applicantAndFormFindByUserKeywordResponses = listOf(applicantAndFormResponses[1])

    @Test
    fun `올바른 지원서 요청에 정상적으로 응답한다`() {
        every { applicationFormService.getApplicationForm(any(), any()) } returns applicationFormResponse

        mockMvc.get("/api/application-forms") {
            param("recruitmentId", "1")
            header(AUTHORIZATION, "Bearer valid_token")
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk }
            content { json(objectMapper.writeValueAsString(ApiResponse.success(applicationFormResponse))) }
        }.andDo {
            handle(
                    MockMvcRestDocumentation.document(
                            "application-forms-read",
                            PayloadDocumentation.responseFields(
                                    PayloadDocumentation.fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                    PayloadDocumentation.fieldWithPath("body").type(JsonFieldType.OBJECT).description("지원서 정보")
                            ).andWithPrefix("body.",
                                    PayloadDocumentation.fieldWithPath("id").type(JsonFieldType.NUMBER).description("아이디"),
                                    PayloadDocumentation.fieldWithPath("recruitmentId").type(JsonFieldType.NUMBER).description("모집 항목 아이디"),
                                    PayloadDocumentation.fieldWithPath("referenceUrl").type(JsonFieldType.STRING).description("참고 URL"),
                                    PayloadDocumentation.fieldWithPath("submitted").type(JsonFieldType.BOOLEAN).description("제출여부"),
                                    PayloadDocumentation.fieldWithPath("answers").type(JsonFieldType.ARRAY).description("지원서 정보"),
                                    PayloadDocumentation.fieldWithPath("answers.[].contents").type(JsonFieldType.STRING).description("지원서 내용"),
                                    PayloadDocumentation.fieldWithPath("answers.[].recruitmentItemId").type(JsonFieldType.NUMBER).description("지원서 아이템 아이디"),
                                    PayloadDocumentation.fieldWithPath("createdDateTime").type(JsonFieldType.STRING).description("생성 시간"),
                                    PayloadDocumentation.fieldWithPath("modifiedDateTime").type(JsonFieldType.STRING).description("수정 시간"),
                                    PayloadDocumentation.fieldWithPath("submittedDateTime").type(JsonFieldType.NULL).description("제출 시간")
                            )
                    )
            )
        }
    }

    @Test
    fun `내 지원서 요청에 정상적으로 응답한다`() {
        every { applicationFormService.getMyApplicationForms(any()) } returns myApplicationFormResponses

        mockMvc.get("/api/application-forms/me") {
            header(AUTHORIZATION, "Bearer valid_token")
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk }
            content { json(objectMapper.writeValueAsString(ApiResponse.success(myApplicationFormResponses))) }
        }.andDo {
            handle(
                    MockMvcRestDocumentation.document(
                            "application-forms-me",
                            PayloadDocumentation.responseFields(
                                    PayloadDocumentation.fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                    PayloadDocumentation.fieldWithPath("body.[]").type(JsonFieldType.ARRAY).description("로그인한 지원자의 지원서 목록")
                            ).andWithPrefix("body.[].", listOf(
                                    PayloadDocumentation.fieldWithPath("recruitmentId").type(JsonFieldType.NUMBER).description("지원서 아이디"),
                                    PayloadDocumentation.fieldWithPath("submitted").type(JsonFieldType.BOOLEAN).description("제출 여부")
                                )
                            )
                    )
            )
        }
    }

    @Test
    fun `특정 모집 id와 지원자에 대한 키워드(이름 or 이메일)로 지원서 정보들을 조회한다`() {
        val recruitmentId = applicantAndFormResponses[0].applicationForm.recruitmentId

        every {
            applicantService.findAllByRecruitmentIdAndKeyword(recruitmentId, userKeyword)
        } returns applicantAndFormFindByUserKeywordResponses

        mockMvc.get("/api/recruitments/{recruitmentId}/application-forms", recruitmentId) {
            contentType = MediaType.APPLICATION_JSON
            header(AUTHORIZATION, "Bearer valid_token")
            param("keyword", userKeyword)
        }
            .andExpect {
                status { isOk }
                content {
                    json(
                        objectMapper.writeValueAsString(
                            ApiResponse.success(
                                applicantAndFormFindByUserKeywordResponses
                            )
                        )
                    )
                }
            }
    }

    @Test
    fun `특정 모집 id에 지원완료한 지원서 정보들을 조회한다`() {
        val recruitmentId = applicantAndFormResponses[0].applicationForm.recruitmentId

        every {
            applicantService.findAllByRecruitmentIdAndKeyword(recruitmentId)
        } returns applicantAndFormResponses

        mockMvc.get(
            "/api/recruitments/{recruitmentId}/application-forms", recruitmentId
        ) {
            contentType = MediaType.APPLICATION_JSON
            header(AUTHORIZATION, "Bearer valid_token")
        }.andExpect {
            status { isOk }
            content { json(objectMapper.writeValueAsString(ApiResponse.success(applicantAndFormResponses))) }
        }
    }
}

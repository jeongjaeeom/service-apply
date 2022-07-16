package apply.ui.api

import apply.application.AssignmentService
import apply.createAssignmentData
import apply.createAssignmentRequest
import apply.createAssignmentResponse
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import support.test.TestEnvironment

@WebMvcTest(
    controllers = [AssignmentRestController::class]
)
@TestEnvironment
internal class AssignmentRestControllerTest : RestControllerTest() {
    @MockkBean
    private lateinit var assignmentService: AssignmentService

    private val recruitmentId = 1L
    private val missionId = 1L

    @Test
    fun `과제 제출물을 제출한다`() {
        every { assignmentService.create(any(), any(), createAssignmentRequest()) } just Runs

        mockMvc.post(
            "/api/recruitments/{recruitmentId}/missions/{missionId}/assignments",
            recruitmentId,
            missionId
        ) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createAssignmentRequest())
            header(HttpHeaders.AUTHORIZATION, "Bearer valid_token")
        }.andExpect {
            status { isOk }
        }.andDo {
            handle(
                    MockMvcRestDocumentation.document(
                            "assignment-submit",
                            PayloadDocumentation.requestFields(
                                    PayloadDocumentation.fieldWithPath("githubUsername").type(JsonFieldType.STRING).description("github 유저네임"),
                                    PayloadDocumentation.fieldWithPath("pullRequestUrl").type(JsonFieldType.STRING).description("PR 링크"),
                                    PayloadDocumentation.fieldWithPath("note").type(JsonFieldType.STRING).description("미션 소감")
                            )
                    )
            )
        }
    }

    @Test
    fun `나의 과제 제출물을 조회한다`() {
        val assignmentResponse = createAssignmentResponse()
        every { assignmentService.getByUserIdAndMissionId(any(), any()) } returns assignmentResponse

        mockMvc.get(
            "/api/recruitments/{recruitmentId}/missions/{missionId}/assignments/me",
            recruitmentId,
            missionId
        ) {
            contentType = MediaType.APPLICATION_JSON
            header(HttpHeaders.AUTHORIZATION, "Bearer valid_token")
        }.andExpect {
            status { isOk }
            content { json(objectMapper.writeValueAsString(ApiResponse.success(assignmentResponse))) }
        }.andDo {
            handle(
                    MockMvcRestDocumentation.document(
                            "assignment-me",
                            PayloadDocumentation.responseFields(
                                    PayloadDocumentation.fieldWithPath("message").type(JsonFieldType.STRING).description("message"),
                                    PayloadDocumentation.fieldWithPath("body").type(JsonFieldType.OBJECT).description("body"),
                                    PayloadDocumentation.fieldWithPath("body.githubUsername").type(JsonFieldType.STRING).description("github 유저네임"),
                                    PayloadDocumentation.fieldWithPath("body.pullRequestUrl").type(JsonFieldType.STRING).description("PR 링크"),
                                    PayloadDocumentation.fieldWithPath("body.note").type(JsonFieldType.STRING).description("미션 소감")
                            )
                    )
            )
        }
    }

    @Test
    fun `과제 제출 내용을 수정한다`() {
        every { assignmentService.update(any(), any(), createAssignmentRequest()) } just Runs

        mockMvc.patch(
                "/api/recruitments/{recruitmentId}/missions/{missionId}/assignments",
                recruitmentId,
                missionId
        ) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createAssignmentRequest())
            header(HttpHeaders.AUTHORIZATION, "Bearer valid_token")
        }.andExpect {
            status { isOk }
        }.andDo {
            handle(
                    MockMvcRestDocumentation.document(
                            "assignment-modify",
                            PayloadDocumentation.requestFields(
                                    PayloadDocumentation.fieldWithPath("githubUsername").type(JsonFieldType.STRING).description("github 유저네임"),
                                    PayloadDocumentation.fieldWithPath("pullRequestUrl").type(JsonFieldType.STRING).description("PR 링크"),
                                    PayloadDocumentation.fieldWithPath("note").type(JsonFieldType.STRING).description("미션 소감")
                            )
                    )
            )
        }
    }

    @Test
    fun `특정 평가 대상자의 특정 과제에 해당하는 과제 제출물을 조회한다`() {
        val assignmentData = createAssignmentData()
        every { assignmentService.findByEvaluationTargetId(any()) } returns assignmentData
        mockMvc.get(
            "/api/recruitments/{recruitmentId}/targets/{targetId}/assignments",
            recruitmentId,
            1L,
        ) {
            header(HttpHeaders.AUTHORIZATION, "Bearer valid_token")
        }.andExpect {
            status { isOk }
            content { json(objectMapper.writeValueAsString(ApiResponse.success(assignmentData))) }
        }
    }
}

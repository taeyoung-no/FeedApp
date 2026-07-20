package com.feedapp.server;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockitoBean
	MemberService memberService;

	@Test
	@DisplayName("유효한 요청이면 회원가입 성공")
	void signup() throws Exception {
		final SignupRequest request = new SignupRequest("username", "password");
		when(memberService.signup(request.username(), request.password()))
			.thenReturn(new MemberResponse(1L, request.username()));

		mockMvc.perform(post("/api/members/signup")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1L))
				.andExpect(jsonPath("$.username").value(request.username()));
	}

	@Test
	@DisplayName("서비스가 예외를 던지면 회원가입 실패")
	void signupWhenServiceThrows() throws Exception {
		final SignupRequest request = new SignupRequest("username", "password");
		when(memberService.signup(request.username(), request.password()))
			.thenThrow(new IllegalArgumentException("username 중복임"));

		mockMvc.perform(post("/api/members/signup")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}
}

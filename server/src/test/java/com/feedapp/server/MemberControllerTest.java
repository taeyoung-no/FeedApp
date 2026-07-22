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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(MemberController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class MemberControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    MemberService memberService;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("유효한 요청이면 회원가입 성공")
    void signup() throws Exception {
        final var request = new SignupRequest("username", "password");
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
        final var request = new SignupRequest("username", "password");
        when(memberService.signup(request.username(), request.password()))
                .thenThrow(new IllegalArgumentException("username 중복임"));

        mockMvc.perform(post("/api/members/signup")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효한 요청이면 로그인 성공")
    void login() throws Exception {
        final var request = new LoginRequest("username", "password");
        when(memberService.login(request.username(), request.password()))
                .thenReturn(new LoginResponse(1L, request.username(), "access-token", "refresh-token"));

        mockMvc.perform(post("/api/members/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value(request.username()))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("서비스가 예외를 던지면 로그인 실패")
    void loginWhenServiceThrows() throws Exception {
        final var request = new LoginRequest("username", "password");
        when(memberService.login(request.username(), request.password()))
                .thenThrow(new IllegalArgumentException("뭔가 잘못 입력함"));

        mockMvc.perform(post("/api/members/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효한 요청이면 토큰 재발급 성공")
    void refresh() throws Exception {
        final String refreshToken = "refresh-token";
        when(memberService.refresh(refreshToken))
                .thenReturn(new TokenResponse("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/api/members/refresh")
                        .contentType(APPLICATION_JSON)
                        .content(refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    @DisplayName("서비스가 예외를 던지면 토큰 재발급 실패")
    void refreshWhenServiceThrows() throws Exception {
        final String refreshToken = "invalid-token";
        when(memberService.refresh(refreshToken))
                .thenThrow(new IllegalArgumentException("뭔가 잘못 입력함"));

        mockMvc.perform(post("/api/members/refresh")
                        .contentType(APPLICATION_JSON)
                        .content(refreshToken))
                .andExpect(status().isBadRequest());
    }
}

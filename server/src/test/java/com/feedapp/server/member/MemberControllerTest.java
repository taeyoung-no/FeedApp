package com.feedapp.server.member;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.feedapp.server.auth.JwtAuthFilter;
import com.feedapp.server.auth.JwtTokenProvider;
import com.feedapp.server.auth.SecurityConfig;
import com.feedapp.server.common.GlobalExceptionHandler;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(MemberController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
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
    @DisplayName("username 중복이면 회원가입 실패")
    void signupWhenServiceThrows() throws Exception {
        final var request = new SignupRequest("username", "password");
        when(memberService.signup(request.username(), request.password()))
                .thenThrow(new ConflictException("username 중복임"));

        mockMvc.perform(post("/api/members/signup")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("username 중복임"));
    }

    @Test
    @DisplayName("유효한 요청이면 로그인 성공")
    void login() throws Exception {
        final var request = new LoginRequest("username", "password");
        when(memberService.login(request.username(), request.password()))
                .thenReturn(new LoginResult(1L, request.username(), "access-token", "refresh-token"));

        mockMvc.perform(post("/api/members/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value(request.username()))
                .andExpect(cookie().value("accessToken", "access-token"))
                .andExpect(cookie().httpOnly("accessToken", true))
                .andExpect(cookie().value("refreshToken", "refresh-token"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    @DisplayName("인증 실패면 로그인 실패")
    void loginWhenServiceThrows() throws Exception {
        final var request = new LoginRequest("username", "password");
        when(memberService.login(request.username(), request.password()))
                .thenThrow(new UnauthorizedException("뭔가 잘못 입력함"));

        mockMvc.perform(post("/api/members/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("뭔가 잘못 입력함"));
    }

    @Test
    @DisplayName("인증된 사용자면 내 정보 조회 성공")
    void me() throws Exception {
        when(memberService.getMe("username")).thenReturn(new LoginResponse(1L, "username"));
        final var auth = new UsernamePasswordAuthenticationToken("username", null, null);

        mockMvc.perform(get("/api/members/me").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("username"));
    }

    @Test
    @DisplayName("인증 없으면 내 정보 조회 실패")
    void meWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("유효하지 않은 인증 정보임"));
    }

    @Test
    @DisplayName("유효한 요청이면 토큰 재발급 성공")
    void refresh() throws Exception {
        final String refreshToken = "refresh-token";
        when(memberService.refresh(refreshToken))
                .thenReturn(new TokenResponse("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/api/members/refresh")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(cookie().value("accessToken", "new-access-token"))
                .andExpect(cookie().httpOnly("accessToken", true))
                .andExpect(cookie().value("refreshToken", "new-refresh-token"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    @DisplayName("인증 실패면 토큰 재발급 실패")
    void refreshWhenServiceThrows() throws Exception {
        final String refreshToken = "invalid-token";
        when(memberService.refresh(refreshToken))
                .thenThrow(new UnauthorizedException("유효하지 않은 인증 정보임"));

        mockMvc.perform(post("/api/members/refresh")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("유효하지 않은 인증 정보임"));
    }

    @Test
    @DisplayName("유효한 요청이면 로그아웃 성공, 토큰 쿠키 삭제")
    void logout() throws Exception {
        final String refreshToken = "refresh-token";

        mockMvc.perform(post("/api/members/logout")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("accessToken", 0))
                .andExpect(cookie().maxAge("refreshToken", 0));

        verify(memberService).logout(refreshToken);
    }

    @Test
    @DisplayName("인증 실패면 로그아웃 실패")
    void logoutWhenServiceThrows() throws Exception {
        final String refreshToken = "invalid-token";
        doThrow(new UnauthorizedException("유효하지 않은 인증 정보임"))
                .when(memberService).logout(refreshToken);

        mockMvc.perform(post("/api/members/logout")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("유효하지 않은 인증 정보임"));
    }
}

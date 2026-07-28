package com.feedapp.server.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.feedapp.server.auth.JwtTokenProvider;
import com.feedapp.server.auth.RefreshTokenStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    RefreshTokenStore refreshTokenStore;

    @Mock
    PasswordEncoder passwordEncoder;

    @Spy
    JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
            "12345678901234567890123456789012",
            3_600_000L
    );

    @InjectMocks
    MemberService memberService;

    @Test
    @DisplayName("유효한 요청이면 정상 저장되고 회원 정보 반환")
    void signup() {
        final String username = "username";
        final String password = "password";
        final String encodedPassword = "encoded-password";
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(memberRepository.save(any(Member.class)))
                .thenReturn(new Member(1L, username, encodedPassword));

        final MemberResponse result = memberService.signup(username, password);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo(username);
        verify(passwordEncoder).encode(password);
        verify(memberRepository).save(argThat((member) ->
                member.getUsername().equals(username) && member.getPassword().equals(encodedPassword)
        ));
    }

    @Test
    @DisplayName("username 중복이면 회원가입 실패")
    void signupWithDuplicateUsername() {
        final String username = "username";
        final String password = "password";
        when(memberRepository.existsByUsername(username)).thenReturn(true);

        assertThatThrownBy(() -> memberService.signup(username, password))
                .isInstanceOf(ConflictException.class);

        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("username 길이가 0이면 회원가입 실패")
    void signupWithEmptyUsername() {
        assertThatThrownBy(() -> memberService.signup("", "password"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("username 길이가 8 초과이면 회원가입 실패")
    void signupWithTooLongUsername() {
        assertThatThrownBy(() -> memberService.signup("long-username", "password"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("password 길이가 0이면 회원가입 실패")
    void signupWithEmptyPassword() {
        assertThatThrownBy(() -> memberService.signup("username", ""))
                .isInstanceOf(IllegalArgumentException.class);

        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("password 길이가 8 초과이면 회원가입 실패")
    void signupWithTooLongPassword() {
        assertThatThrownBy(() -> memberService.signup("username", "long-password"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("유효한 요청이면 로그인 성공, 액세스/리프레시 토큰과 회원 정보를 반환, sid-jti 저장")
    void login() {
        final String username = "username";
        final String password = "password";
        final String encodedPassword = "encoded-password";
        final var member = new Member(1L, username, encodedPassword);
        when(memberRepository.findByUsername(username)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);

        final LoginResult result = memberService.login(username, password);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(jwtTokenProvider.getType(result.getAccessToken())).isEqualTo("access");
        assertThat(jwtTokenProvider.getType(result.getRefreshToken())).isEqualTo("refresh");
        assertThat(jwtTokenProvider.getUsername(result.getRefreshToken())).isEqualTo(username);
        verify(passwordEncoder).matches(password, encodedPassword);

        final String sid = jwtTokenProvider.getSid(result.getRefreshToken());
        final String jti = jwtTokenProvider.getJti(result.getRefreshToken());
        verify(refreshTokenStore).save(sid, jti);
    }

    @Test
    @DisplayName("유효한 리프레시 토큰이면 새 액세스/리프레시 토큰 발급, jti 갱신")
    void refresh() {
        final String username = "username";
        final String oldRefreshToken = jwtTokenProvider.createRefreshToken(username);
        final String sid = jwtTokenProvider.getSid(oldRefreshToken);
        final String oldJti = jwtTokenProvider.getJti(oldRefreshToken);
        when(refreshTokenStore.find(sid)).thenReturn(Optional.of(oldJti));

        final TokenResponse result = memberService.refresh(oldRefreshToken);

        assertThat(jwtTokenProvider.getType(result.getAccessToken())).isEqualTo("access");
        assertThat(jwtTokenProvider.getType(result.getRefreshToken())).isEqualTo("refresh");
        assertThat(jwtTokenProvider.getUsername(result.getAccessToken())).isEqualTo(username);
        assertThat(jwtTokenProvider.getSid(result.getRefreshToken())).isEqualTo(sid);
        assertThat(jwtTokenProvider.getJti(result.getRefreshToken())).isNotEqualTo(oldJti);
        verify(refreshTokenStore).save(eq(sid), argThat((jti) -> !jti.equals(oldJti)));
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰이면 재발급 실패")
    void refreshWithInvalidToken() {
        assertThatThrownBy(() -> memberService.refresh("invalid-token"))
                .isInstanceOf(UnauthorizedException.class);

        verify(refreshTokenStore, never()).save(anyString(), anyString());
    }

    @Test
    @DisplayName("액세스 토큰으로 재발급하면 실패")
    void refreshWithAccessToken() {
        final String accessToken = jwtTokenProvider.createAccessToken("username");

        assertThatThrownBy(() -> memberService.refresh(accessToken))
                .isInstanceOf(UnauthorizedException.class);

        verify(refreshTokenStore, never()).save(anyString(), anyString());
    }

    @Test
    @DisplayName("저장소에 sid가 없으면 재발급 실패")
    void refreshWhenSessionMissing() {
        final String refreshToken = jwtTokenProvider.createRefreshToken("username");
        when(refreshTokenStore.find(jwtTokenProvider.getSid(refreshToken)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.refresh(refreshToken))
                .isInstanceOf(UnauthorizedException.class);

        verify(refreshTokenStore, never()).save(anyString(), anyString());
    }

    @Test
    @DisplayName("저장된 jti와 다르면 재발급 실패")
    void refreshWithMismatchedJti() {
        final String refreshToken = jwtTokenProvider.createRefreshToken("username");
        final String sid = jwtTokenProvider.getSid(refreshToken);
        when(refreshTokenStore.find(sid)).thenReturn(Optional.of(UUID.randomUUID().toString()));

        assertThatThrownBy(() -> memberService.refresh(refreshToken))
                .isInstanceOf(UnauthorizedException.class);

        verify(refreshTokenStore, never()).save(anyString(), anyString());
    }

    @Test
    @DisplayName("username이 일치하지 않으면 로그인 실패")
    void loginWithWrongUsername() {
        final String username = "username";
        when(memberRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.login(username, "password"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("password가 일치하지 않으면 로그인 실패")
    void loginWithWrongPassword() {
        final String username = "username";
        final String encodedPassword = "encoded-password";
        when(memberRepository.findByUsername(username))
                .thenReturn(Optional.of(new Member(1L, username, encodedPassword)));
        when(passwordEncoder.matches("wrongpwd", encodedPassword)).thenReturn(false);

        assertThatThrownBy(() -> memberService.login(username, "wrongpwd"))
                .isInstanceOf(UnauthorizedException.class);

        verify(passwordEncoder).matches("wrongpwd", encodedPassword);
        verify(refreshTokenStore, never()).save(anyString(), anyString());
    }

    @Test
    @DisplayName("username 길이가 0이면 로그인 실패")
    void loginWithEmptyUsername() {
        assertThatThrownBy(() -> memberService.login("", "password"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("username 길이가 8 초과이면 로그인 실패")
    void loginWithTooLongUsername() {
        assertThatThrownBy(() -> memberService.login("long-username", "password"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("password 길이가 0이면 로그인 실패")
    void loginWithEmptyPassword() {
        assertThatThrownBy(() -> memberService.login("username", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("password 길이가 8 초과이면 로그인 실패")
    void loginWithTooLongPassword() {
        assertThatThrownBy(() -> memberService.login("username", "long-password"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("유효한 리프레시 토큰이면 세션 삭제")
    void logout() {
        final String refreshToken = jwtTokenProvider.createRefreshToken("username");
        final String sid = jwtTokenProvider.getSid(refreshToken);

        memberService.logout(refreshToken);

        verify(refreshTokenStore).delete(sid);
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰이면 로그아웃 실패")
    void logoutWithInvalidToken() {
        assertThatThrownBy(() -> memberService.logout("invalid-token"))
                .isInstanceOf(UnauthorizedException.class);

        verify(refreshTokenStore, never()).delete(anyString());
    }

    @Test
    @DisplayName("액세스 토큰으로 로그아웃하면 실패")
    void logoutWithAccessToken() {
        final String accessToken = jwtTokenProvider.createAccessToken("username");

        assertThatThrownBy(() -> memberService.logout(accessToken))
                .isInstanceOf(UnauthorizedException.class);

        verify(refreshTokenStore, never()).delete(anyString());
    }
}


package com.feedapp.server.member;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MemberController {

    private final MemberService memberService;
    private final int accessTokenMaxAgeSeconds;
    private final int refreshTokenMaxAgeSeconds;

    public MemberController(
            MemberService memberService,
            @Value("${jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs
    ) {
        this.memberService = memberService;
        this.accessTokenMaxAgeSeconds = toSeconds(accessExpirationMs);
        this.refreshTokenMaxAgeSeconds = toSeconds(refreshExpirationMs);
    }

    @PostMapping("/api/members/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse signup(@RequestBody SignupRequest request) {
        return memberService.signup(request.username(), request.password());
    }

    @PostMapping("/api/members/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@RequestBody LoginRequest request, HttpServletResponse response) {
        final LoginResult loginResult = memberService.login(request.username(), request.password());
        addTokenCookies(response, loginResult.getAccessToken(), loginResult.getRefreshToken());
        return new LoginResponse(loginResult.getId(), loginResult.getUsername());
    }

    @GetMapping("/api/members/me")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse me(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName().equals("anonymousUser")) {
            throw new UnauthorizedException("유효하지 않은 인증 정보임");
        }
        return memberService.getMe(authentication.getName());
    }

    @PostMapping("/api/members/refresh")
    @ResponseStatus(HttpStatus.OK)
    public void refresh(
            @CookieValue("refreshToken") String refreshToken,
            HttpServletResponse response
    ) {
        final TokenResponse tokenResponse = memberService.refresh(refreshToken);
        addTokenCookies(response, tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());
    }

    @PostMapping("/api/members/logout")
    @ResponseStatus(HttpStatus.OK)
    public void logout(
            @CookieValue("refreshToken") String refreshToken,
            HttpServletResponse response
    ) {
        memberService.logout(refreshToken);
        clearTokenCookies(response);
    }

    private void addTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addCookie(httpOnlyCookie("accessToken", accessToken, accessTokenMaxAgeSeconds));
        response.addCookie(httpOnlyCookie("refreshToken", refreshToken, refreshTokenMaxAgeSeconds));
    }

    private void clearTokenCookies(HttpServletResponse response) {
        response.addCookie(expiredCookie("accessToken"));
        response.addCookie(expiredCookie("refreshToken"));
    }

    private Cookie httpOnlyCookie(String name, String value, int maxAgeSeconds) {
        final Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        return cookie;
    }

    private Cookie expiredCookie(String name) {
        return httpOnlyCookie(name, "", 0);
    }

    private static int toSeconds(long expirationMs) {
        return (int) (expirationMs / 1000);
    }
}

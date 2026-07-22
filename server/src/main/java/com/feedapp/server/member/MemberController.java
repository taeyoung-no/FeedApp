package com.feedapp.server.member;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

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

    @PostMapping("/api/members/refresh")
    @ResponseStatus(HttpStatus.OK)
    public void refresh(
            @CookieValue("refreshToken") String refreshToken,
            HttpServletResponse response
    ) {
        final TokenResponse tokenResponse = memberService.refresh(refreshToken);
        addTokenCookies(response, tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleIllegalArgumentException(IllegalArgumentException exception) {
    }

    private void addTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addCookie(httpOnlyCookie("accessToken", accessToken));
        response.addCookie(httpOnlyCookie("refreshToken", refreshToken));
    }

    private Cookie httpOnlyCookie(String name, String value) {
        final Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        return cookie;
    }
}

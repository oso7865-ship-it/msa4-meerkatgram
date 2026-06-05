package com.msa4meerkatgram.domain.auth.controllers;

import com.msa4meerkatgram.domain.auth.requests.LoginRequest;
import com.msa4meerkatgram.domain.auth.requests.RegistrationReq;
import com.msa4meerkatgram.domain.auth.responses.AuthRes;
import com.msa4meerkatgram.domain.auth.services.AuthService;
import com.msa4meerkatgram.global.responses.GlobalResponse;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<GlobalResponse<AuthRes>> login(
            @Valid @RequestBody LoginRequest loginRequest
            , HttpServletResponse response
    ){
        return ResponseEntity.status(200).body(
            GlobalResponse.<AuthRes>builder()
                .code("00")
                .message("로그인 성공")
                .data(authService.login(response, loginRequest))
                .build()
        );
    }

    @PostMapping("/reissue-token")
    public ResponseEntity<GlobalResponse<AuthRes>> reissue(
            HttpServletRequest request
            ,HttpServletResponse response
    ) {
        return ResponseEntity.status(200).body(
            GlobalResponse.<AuthRes>builder()
                .code("00")
                .message("토큰 재발급 완료")
                .data(authService.reissue(request, response))
                .build()
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<GlobalResponse<String>> logout(
        HttpServletResponse response
        , @AuthenticationPrincipal Claims claims
    ) {
        authService.logout(response, Long.parseLong(claims.getSubject()));

        return ResponseEntity.status(200).body(
            GlobalResponse.<String>builder()
                .code("00")
                .message("로그아웃 완료")
                .build()
        );
    }
    @PostMapping("/registration")
    public ResponseEntity<GlobalResponse<String>> registration(
        @Valid @RequestBody RegistrationReq registrationReq
    ) {
        authService.registration(registrationReq);

        return ResponseEntity.status(200).body(
            GlobalResponse.<String>builder()
                .code("00")
                .message("회원가입 완료")
                .build()
        );
    }

}

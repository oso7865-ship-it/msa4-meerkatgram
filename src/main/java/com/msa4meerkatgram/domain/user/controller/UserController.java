package com.msa4meerkatgram.domain.user.controller;

import com.msa4meerkatgram.domain.auth.responses.AuthRes;
import com.msa4meerkatgram.domain.user.services.UserService;
import com.msa4meerkatgram.global.responses.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {
    private final UserService userService;

    @GetMapping("/test")
    public ResponseEntity<GlobalResponse<AuthRes>> test() {
        return ResponseEntity.status(200).body(
                GlobalResponse.<AuthRes>builder()
                        .code("00")
                        .message("정상처리")
                        .data(userService.test())
                        .build()
        );
    }
}

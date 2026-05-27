package com.msa4meerkatgram.domain.auth.services;

import com.msa4meerkatgram.domain.auth.requests.LoginRequest;
import com.msa4meerkatgram.domain.user.entities.User;
import com.msa4meerkatgram.domain.user.mapper.UserMapper;
import com.msa4meerkatgram.global.errors.custom.NotRegisteredException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;

    public void login(LoginRequest loginRequest) {
        // 유저정보 흭득
        User user = userMapper.findByEmail(loginRequest.email());

        // 유저 가입 여부 확인
        if(user == null){
            throw new NotRegisteredException("가입되지 않은 이메일입니다.");
        }
        
        // 비밀번호 체크
        
        // 토큰 생성
        
        // 리프래시 토큰을 DB에 저장
        
        // 리프래시 토큰을 Cookie에 저장
        
        // 리턴
        
        
    }
}

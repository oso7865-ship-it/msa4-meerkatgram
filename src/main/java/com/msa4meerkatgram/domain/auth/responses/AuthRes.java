package com.msa4meerkatgram.domain.auth.responses;


import com.msa4meerkatgram.domain.user.entities.User;
import com.msa4meerkatgram.domain.user.responses.UserWithPostCountRes;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "로그인 레스폰스")
@Builder
public record AuthRes(
        UserWithPostCountRes userWithPostCountRes
        , String accessToken
) {
    public static AuthRes from (User user, String accessToken, long countPosts) {
        return new AuthRes(
            UserWithPostCountRes.from(user, countPosts),
            accessToken
        );
    }
}

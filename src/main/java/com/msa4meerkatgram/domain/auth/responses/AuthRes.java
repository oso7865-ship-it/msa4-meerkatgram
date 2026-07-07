package com.msa4meerkatgram.domain.auth.responses;


import com.msa4meerkatgram.domain.user.entities.User;
import com.msa4meerkatgram.domain.user.responses.UserWithPostCountRes;
import lombok.Builder;

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

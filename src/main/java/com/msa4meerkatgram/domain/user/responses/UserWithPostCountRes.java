package com.msa4meerkatgram.domain.user.responses;

import com.msa4meerkatgram.domain.user.entities.User;
import lombok.Builder;

@Builder
public record UserWithPostCountRes(
    UserRes userRes,
    long countPosts
) {

    public static UserWithPostCountRes from (User user, long countPost) {
        return new UserWithPostCountRes(
            UserRes.from(user),
            countPost
        );
    }
}

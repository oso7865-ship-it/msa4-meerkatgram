package com.msa4meerkatgram.domain.user.responses;

import com.msa4meerkatgram.domain.user.entities.User;
import lombok.Builder;

@Builder
public record UserWithPostCountRes(
    UserRes userRes,
    Long countPosts
) {

    public static UserWithPostCountRes from (User user, Long countPost) {
        return new UserWithPostCountRes(
            UserRes.from(user),
            countPost
        );
    }
}

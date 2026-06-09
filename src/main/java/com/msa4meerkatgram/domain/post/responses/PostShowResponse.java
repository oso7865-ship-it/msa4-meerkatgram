package com.msa4meerkatgram.domain.post.responses;

import lombok.Builder;

@Builder
public record PostShowResponse(
    long id,
    long userId,
    String content,
    String image,
    long likeCount,
    boolean liked
) {
}
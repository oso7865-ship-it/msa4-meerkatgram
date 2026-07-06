package com.msa4meerkatgram.domain.post.entities;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Like {
    Long id;
    Long userId;
    Long postId;
    String createdAt;
    String updatedAt;
    String deletedAt;
}

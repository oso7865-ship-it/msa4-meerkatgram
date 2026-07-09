package com.msa4meerkatgram.domain.post.responses;

import com.msa4meerkatgram.domain.post.entities.Post;
import lombok.Builder;

import java.util.List;

@Builder
public record PostIndexResponse(
    Long total,
    boolean lastPage,
    List<PostWithUserRes> posts
) {
    public static PostIndexResponse from (Long total, boolean lastPage, List<Post> posts) {
        return new PostIndexResponse(
            total,
            lastPage,
            posts.stream().map(PostWithUserRes::from).toList()
        );
    }
}

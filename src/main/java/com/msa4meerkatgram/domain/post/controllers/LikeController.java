package com.msa4meerkatgram.domain.post.controllers;

import com.msa4meerkatgram.domain.post.services.LikeService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/{postId}/likes")
    public boolean toggleLike(
        @PathVariable long postId,
        @AuthenticationPrincipal Claims claims
        ) {
        long userId = Long.parseLong(claims.getSubject());

        return likeService.toggleLike(userId, postId);
    }

    @GetMapping("/{postId}/likes/count")
    public long countLikes(@PathVariable long postId) {
        return likeService.countLikesByPostId(postId);
    }
}
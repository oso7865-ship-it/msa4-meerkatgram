package com.msa4meerkatgram.domain.post.controllers;

import com.msa4meerkatgram.domain.post.requests.PostCreateReq;
import com.msa4meerkatgram.domain.post.responses.PostWithUserRes;
import com.msa4meerkatgram.domain.post.services.PostService;
import com.msa4meerkatgram.global.responses.GlobalResponse;
import io.jsonwebtoken.Claims;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class PostController {
    private final PostService postService;

    // @GetMapping("/posts")
    // public ResponseEntity<GlobalResponse<PostIndexResponse>> index(PostIndexRequest postIndexRequest) {
    //     PostIndexResponse postIndexResponse = postService.index(postIndexRequest);
    //     return ResponseEntity.status(200).body(
    //         GlobalResponse.<PostIndexResponse>builder()
    //            .code("00")
    //            .message("정상처리")
    //            .data(postIndexResponse)
    //            .build()
    //     );
    // }

    @GetMapping("/posts/{id}")
    public ResponseEntity<GlobalResponse<PostWithUserRes>> show(
        @Min(value = 1, message = "1이상 숫자만 허용합니다.")
        @PathVariable long id
    ) {
        PostWithUserRes result = postService.show(id);
        return ResponseEntity.status(200).body(
            GlobalResponse.<PostWithUserRes>builder()
                .code("00")
                .message("정상처리")
                .data(result)
                .build()
        );
    }

    @PostMapping("/postCreate")
    public ResponseEntity<GlobalResponse<PostCreateReq>> postCreate(
        @RequestParam String content,
        @RequestParam MultipartFile file,
        @AuthenticationPrincipal Claims claims
    ) {
        long userId = Long.parseLong(claims.getSubject());

        postService.postCreates(content, userId, file);

        return ResponseEntity.ok(
            GlobalResponse.<PostCreateReq>builder()
                .code("00")
                .message("정상처리")
                .data(null)
                .build()
        );
    }
    @DeleteMapping("/postDelete/{postId}")
    public ResponseEntity<GlobalResponse<Void>> deletePost(
        @PathVariable long postId,
        @AuthenticationPrincipal Claims claims
    ) {
        long userId = Long.parseLong(claims.getSubject());

        postService.deletePost(postId, userId);

        return ResponseEntity.ok(
            GlobalResponse.<Void>builder()
                .code("00")
                .message("정상처리")
                .data(null)
                .build()
        );
    }

}

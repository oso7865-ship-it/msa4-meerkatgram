package com.msa4meerkatgram.domain.post.controllers;

import com.msa4meerkatgram.domain.post.requests.PostCreateReq;
import com.msa4meerkatgram.domain.post.requests.PostIndexRequest;
import com.msa4meerkatgram.domain.post.responses.PostIndexResponse;
import com.msa4meerkatgram.domain.post.responses.PostWithUserRes;
import com.msa4meerkatgram.domain.post.services.PostService;
import com.msa4meerkatgram.global.annotaions.openapi.ApiNotValidErrorResponse;
import com.msa4meerkatgram.global.responses.GlobalResponse;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "게시글 API", description = "게시글 관련")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class PostController {
    private final PostService postService;

    @ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공")
    @ApiNotValidErrorResponse
    @GetMapping("/posts")
    public ResponseEntity<GlobalResponse<PostIndexResponse>> index(PostIndexRequest postIndexRequest) {
        return GlobalResponse.success(postService.index(postIndexRequest));
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<GlobalResponse<PostWithUserRes>> show(
        @Parameter(description = "게시글 번호", example = "1") @Min(value = 1, message = "1이상 숫자만 허용합니다.") @PathVariable long id
    ) {
        return GlobalResponse.success(postService.show(id));
    }

    @PostMapping("/postCreate")
    public ResponseEntity<GlobalResponse<PostCreateReq>> postCreate(
        @RequestParam String content,
        @RequestParam MultipartFile file,
        @AuthenticationPrincipal Claims claims
    ) {
        long userId = Long.parseLong(claims.getSubject());

        postService.postCreates(content, userId, file);

        return GlobalResponse.success(null);
    }
    @DeleteMapping("/postDelete/{postId}")
    public ResponseEntity<GlobalResponse<Void>> deletePost(
        @PathVariable long postId,
        @AuthenticationPrincipal Claims claims
    ) {
        long userId = Long.parseLong(claims.getSubject());

        postService.deletePost(postId, userId);

        return GlobalResponse.success();
    }

}

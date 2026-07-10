package com.msa4meerkatgram.domain.post.controllers;

import com.msa4meerkatgram.domain.post.requests.PostCreateReq;
import com.msa4meerkatgram.domain.post.requests.PostIndexRequest;
import com.msa4meerkatgram.domain.post.responses.PostIndexResponse;
import com.msa4meerkatgram.domain.post.responses.PostWithUserRes;
import com.msa4meerkatgram.domain.post.services.PostService;
import com.msa4meerkatgram.global.config.openapi.CustomApiResponse;
import com.msa4meerkatgram.global.responses.GlobalResponse;
import com.msa4meerkatgram.global.responses.constant.CustomResponseCode;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(summary = "게시글 목록 조회 처리", description = "페이지와 리미트 설정")
    @CustomApiResponse(value = {
        CustomResponseCode.NOT_FOUND_DATA_ERROR,
        CustomResponseCode.INVALID_PARAMETER_ERROR,
        CustomResponseCode.DB_ERROR,
        CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/posts")
    public ResponseEntity<GlobalResponse<PostIndexResponse>> index(PostIndexRequest postIndexRequest) {
        return GlobalResponse.success(postService.index(postIndexRequest));
    }

    @Operation(summary = "특정 게시글 조회 처리", description = "특정 게시글 번호 입력")
    @CustomApiResponse(value = {
        CustomResponseCode.NOT_FOUND_DATA_ERROR,
        CustomResponseCode.INVALID_PARAMETER_ERROR,
        CustomResponseCode.DB_ERROR,
        CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/posts/{id}")
    public ResponseEntity<GlobalResponse<PostWithUserRes>> show(
        @Parameter(description = "게시글 번호", example = "1") @Min(value = 1, message = "1이상 숫자만 허용합니다.") @PathVariable long id
    ) {
        return GlobalResponse.success(postService.show(id));
    }


    @Operation(summary = "게시글 생성 처리")
    @CustomApiResponse(value = {
        CustomResponseCode.NOT_FOUND_DATA_ERROR,
        CustomResponseCode.INVALID_PARAMETER_ERROR,
        CustomResponseCode.DB_ERROR,
        CustomResponseCode.SYSTEM_ERROR
    })
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

    @Operation(summary = "게시글 삭제 처리", description = "게시글 번호 입력")
    @CustomApiResponse(value = {
        CustomResponseCode.DUPLICATED_DATA_ERROR,
        CustomResponseCode.INVALID_PARAMETER_ERROR,
        CustomResponseCode.DB_ERROR,
        CustomResponseCode.SYSTEM_ERROR
    })
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

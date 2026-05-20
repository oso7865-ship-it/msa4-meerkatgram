package com.msa4meerkatgram.domain.post.controllers;

import com.msa4meerkatgram.domain.post.requests.PostIndexRequest;
import com.msa4meerkatgram.domain.post.responses.PostIndexResponse;
import com.msa4meerkatgram.domain.post.services.PostService;
import com.msa4meerkatgram.global.responses.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class PostController {
    private final PostService postService;

    @GetMapping("/posts")
    public ResponseEntity<GlobalResponse<PostIndexResponse>> index(PostIndexRequest postIndexRequest) {
        PostIndexResponse postIndexResponse = postService.index(postIndexRequest);
        return ResponseEntity.status(200).body(
            GlobalResponse.<PostIndexResponse>builder()
                   .code("00")
                   .message("정상처리")
                   .data(postIndexResponse)
                   .build()
        );
    }
}

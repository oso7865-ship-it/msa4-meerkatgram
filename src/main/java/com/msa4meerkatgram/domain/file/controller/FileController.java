package com.msa4meerkatgram.domain.file.controller;

import com.msa4meerkatgram.domain.file.responses.FileRes;
import com.msa4meerkatgram.domain.file.service.FileService;
import com.msa4meerkatgram.global.responses.GlobalResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "파일 API", description = "파일 업로드 관련")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class FileController {
    private final FileService fileService;

    @PostMapping("/files/profiles")
    public ResponseEntity<GlobalResponse<FileRes>> storeProfile(
        @ModelAttribute MultipartFile file
    ) {
       return ResponseEntity.status(200).body(
           GlobalResponse.<FileRes>builder()
               .code("00")
               .message("파일 저장 성공")
               .data(fileService.storeProfile(file))
               .build()
       );
    }

    @PostMapping("/files/posts")
    public ResponseEntity<GlobalResponse<FileRes>> storePosts(
        @ModelAttribute MultipartFile file
    ) {
        return ResponseEntity.status(200).body(
            GlobalResponse.<FileRes>builder()
                .code("00")
                .message("파일 저장 성공")
                .data(fileService.storePosts(file))
                .build()
        );
    }
}

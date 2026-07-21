package com.msa4meerkatgram.domain.file.controller;

import com.msa4meerkatgram.domain.file.responses.FileRes;
import com.msa4meerkatgram.domain.file.service.FileService;
import com.msa4meerkatgram.global.config.openapi.CustomApiResponse;
import com.msa4meerkatgram.global.responses.GlobalResponse;
import com.msa4meerkatgram.global.responses.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "프로필 파일 업로드 처리")
    @CustomApiResponse(value = {
        CustomResponseCode.NOT_FOUND_DATA_ERROR,
        CustomResponseCode.FILE_MANAGED_ERROR,
        CustomResponseCode.DB_ERROR,
        CustomResponseCode.SYSTEM_ERROR
    })
    @PostMapping("/files/profiles")
    public ResponseEntity<GlobalResponse<FileRes>> storeProfile(
        @ModelAttribute MultipartFile file
    ) {
       return GlobalResponse.success(fileService.storeProfile(file));
    }


    @Operation(summary = "포스트 파일 업로드 처리")
    @CustomApiResponse(value = {
        CustomResponseCode.NOT_FOUND_DATA_ERROR,
        CustomResponseCode.FILE_MANAGED_ERROR,
        CustomResponseCode.DB_ERROR,
        CustomResponseCode.SYSTEM_ERROR
    })
    @PostMapping("/files/posts")
    public ResponseEntity<GlobalResponse<FileRes>> storePosts(
        @ModelAttribute MultipartFile file
    ) {
        return GlobalResponse.success(fileService.storePosts(file));
    }
}

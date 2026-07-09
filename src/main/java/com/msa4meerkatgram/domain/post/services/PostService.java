package com.msa4meerkatgram.domain.post.services;

import com.msa4meerkatgram.domain.file.responses.FileRes;
import com.msa4meerkatgram.domain.file.service.FileService;
import com.msa4meerkatgram.domain.post.entities.Post;
import com.msa4meerkatgram.domain.post.repositories.PostQueryRepository;
import com.msa4meerkatgram.domain.post.repositories.PostRepository;
import com.msa4meerkatgram.domain.post.requests.PostIndexRequest;
import com.msa4meerkatgram.domain.post.responses.PostIndexResponse;
import com.msa4meerkatgram.domain.post.responses.PostWithUserRes;
import com.msa4meerkatgram.domain.user.entities.User;
import com.msa4meerkatgram.domain.user.repositories.UserRepository;
import com.msa4meerkatgram.global.errors.custom.DeletedRecordException;
import com.msa4meerkatgram.global.errors.custom.InvalidPostCreateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Service
@RequiredArgsConstructor
public class PostService {
    private final FileService fileService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostQueryRepository postQueryRepository;

    public PostIndexResponse index(PostIndexRequest postIndexRequest) {
        int offset = (postIndexRequest.page() - 1) * postIndexRequest.limit();
        // 특정 페이지 게시글 조회
        List<Post> result = postQueryRepository.pagination(offset, postIndexRequest.limit());
        // 토탈 획득
        long total = postRepository.count();
        boolean lastPage = offset + postIndexRequest.limit() >= total;
        // 컨트롤러 전달
        return PostIndexResponse.from(total, lastPage, result);
    }


    public PostWithUserRes show(Long id) {
        Post result = postRepository.findById(id)
                          .orElseThrow(() -> new DeletedRecordException("이미 삭제된 게시글입니다."));

        return PostWithUserRes.from(result);
    }

    @Transactional(rollbackFor = Exception.class)
    public void postCreates(String content, Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("접근방법이 올바르지 않습니다."));

        if (content == null || content.isBlank()) {
            throw new InvalidPostCreateException("게시글 내용을 입력해주세요.");
        }

        if (file == null || file.isEmpty()) {
            throw new InvalidPostCreateException("게시글 이미지를 첨부해주세요.");
        }

        FileRes fileRes = fileService.storePosts(file);
        String image = fileRes.fileUri();

        // Post post = Post.builder()
        //                 .userId(userId)
        //                 .content(content)
        //                 .image(image)
        //                 .build();
        Post post = new Post();
        post.setImage(image);
        post.setContent(content);
        post.setUser(user);
        postRepository.save(post);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePost(long id, long userId) {
        User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("접근방법이 올바르지 않습니다."));


        // long result = postMapper.deletePost(id, user);
        Post post = postRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("삭제할 게시글이 없거나 삭제 권한이 없습니다."));

        if (!post.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("삭제 권한이 없습니다.");
        }

        postRepository.delete(post);
    }

}
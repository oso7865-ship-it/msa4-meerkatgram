package com.msa4meerkatgram.domain.post.services;

import com.msa4meerkatgram.domain.file.responses.FileRes;
import com.msa4meerkatgram.domain.file.service.FileService;
import com.msa4meerkatgram.domain.post.entities.LikeMyBatis;
import com.msa4meerkatgram.domain.post.entities.PostMybatis;
import com.msa4meerkatgram.domain.post.mapper.LikeMapper;
import com.msa4meerkatgram.domain.post.mapper.PostMapper;
import com.msa4meerkatgram.domain.post.requests.PostIndexRequest;
import com.msa4meerkatgram.domain.post.responses.PostIndexResponse;
import com.msa4meerkatgram.domain.post.responses.PostShowResponse;
import com.msa4meerkatgram.domain.user.entities.UserMybatis;
import com.msa4meerkatgram.domain.user.mapper.UserMapper;
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
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final LikeMapper likeMapper;
    private final FileService fileService;

    public PostIndexResponse index(PostIndexRequest postIndexRequest) {
        int offset = (postIndexRequest.page() - 1) * postIndexRequest.limit();
        // 특정 페이지 게시글 조회
        List<PostMybatis> posts = postMapper.getPagination(postIndexRequest.limit(), offset);

        // 토탈 획득
        long total = postMapper.getTotal();
        boolean lastPage = offset + postIndexRequest.limit() >= total;

        // 컨트롤러 전달
        return PostIndexResponse.builder()
           .total(total)
           .lastPage(lastPage)
           .posts(posts)
           .build();
    }

    public PostShowResponse show(long id, long userId) {
        PostMybatis post = postMapper.findByPk(id);

        if (post == null) {
            throw new DeletedRecordException("이미 삭제된 게시글입니다.");
        }

        long likeCount = likeMapper.countLikesByPostId(id);

        LikeMyBatis like = likeMapper.findByUserIdAndPostId(userId, id);
        boolean liked = like != null && like.getDeletedAt() == null;

        return PostShowResponse.builder()
                   .id(post.getId())
                   .userId(post.getUserId())
                   .content(post.getContent())
                   .image(post.getImage())
                   .likeCount(likeCount)
                   .liked(liked)
                   .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void postCreates(String content, long userId, MultipartFile file) {
        UserMybatis user = userMapper.findByPk(userId);

        if (user == null) {
            throw new RuntimeException("접근방법이 올바르지 않습니다.");
        }

        if (content == null || content.isBlank()) {
            throw new InvalidPostCreateException("게시글 내용을 입력해주세요.");
        }

        if (file == null || file.isEmpty()) {
            throw new InvalidPostCreateException("게시글 이미지를 첨부해주세요.");
        }

        FileRes fileRes = fileService.storePosts(file);
        String image = fileRes.fileUri();

        PostMybatis post = PostMybatis.builder()
                        .userId(userId)
                        .content(content)
                        .image(image)
                        .build();

        postMapper.postCreate(post);

    }
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(long id, long userId) {
        UserMybatis user = userMapper.findByPk(userId);

        if (user == null) {
            throw new RuntimeException("접근방법이 올바르지 않습니다.");
        }

        long result = postMapper.deletePost(id, userId);

        if (result == 0) {
            throw new RuntimeException("삭제할 게시글이 없거나 삭제 권한이 없습니다.");
        }
    }

    public PostShowResponse getPost(long id) {

        PostMybatis post = postMapper.findByPk(id);

        long likeCount = likeMapper.countLikesByPostId(id);

        return PostShowResponse.builder()
                   .id(post.getId())
                   .userId(post.getUserId())
                   .content(post.getContent())
                   .image(post.getImage())
                   .likeCount(likeCount)
                   .build();
    }


}

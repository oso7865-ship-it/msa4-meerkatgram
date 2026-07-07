package com.msa4meerkatgram.domain.post.mapper;

import com.msa4meerkatgram.domain.post.entities.LikeMyBatis;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LikeMapper {
    LikeMyBatis findByUserIdAndPostId(long userId, long postId);
    long createLike(LikeMyBatis like);
    long restoreLike(long userId, long postId);
    long deleteLike(long userId, long postId);
    long countLikesByPostId(long postId);
}

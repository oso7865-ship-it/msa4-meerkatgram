package com.msa4meerkatgram.domain.post.mapper;

import com.msa4meerkatgram.domain.post.entities.Like;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LikeMapper {
    Like findByUserIdAndPostId(long userId, long postId);
    long createLike(Like like);
    long restoreLike(long userId, long postId);
    long deleteLike(long userId, long postId);
    long countLikesByPostId(long postId);
}

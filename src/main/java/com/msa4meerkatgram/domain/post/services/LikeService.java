package com.msa4meerkatgram.domain.post.services;

import com.msa4meerkatgram.domain.post.entities.Like;
import com.msa4meerkatgram.domain.post.mapper.LikeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

    @Service
    @RequiredArgsConstructor
    public class LikeService {

        private final LikeMapper likeMapper;

        @Transactional
        public boolean toggleLike(long userId, long postId) {

            Like like = likeMapper.findByUserIdAndPostId(userId, postId);

            if (like == null) {
                Like newLike = Like.builder()
                                   .userId(userId)
                                   .postId(postId)
                                   .build();

                likeMapper.createLike(newLike);
                return true;
            }

            if (like.getDeletedAt() != null) {
                likeMapper.restoreLike(userId, postId);
                return true;
            }

            likeMapper.deleteLike(userId, postId);
            return false;
        }

        public long countLikesByPostId(long postId) {
            return likeMapper.countLikesByPostId(postId);
        }
    }
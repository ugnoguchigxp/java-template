package com.example.javastandard.db.mapper;

import com.example.javastandard.db.model.RefreshTokenRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefreshTokenMapper {
    int insert(RefreshTokenRecord token);
    RefreshTokenRecord findByHash(@Param("tokenHash") String tokenHash);
    int consumeIfUnexpired(@Param("tokenHash") String tokenHash, @Param("now") long now);
    int deleteByHash(@Param("tokenHash") String tokenHash);
    int deleteExpired(@Param("now") long now);
}

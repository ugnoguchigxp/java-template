package com.example.javastandard.db.repository;

import com.example.javastandard.db.mapper.RefreshTokenMapper;
import com.example.javastandard.db.model.RefreshTokenRecord;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRepository {
    private final RefreshTokenMapper mapper;

    public RefreshTokenRepository(RefreshTokenMapper mapper) {
        this.mapper = mapper;
    }

    public void insert(RefreshTokenRecord token) {
        mapper.insert(token);
    }

    public RefreshTokenRecord findByHash(String hash) {
        return mapper.findByHash(hash);
    }

    public int consumeIfUnexpired(String hash, long now) {
        return mapper.consumeIfUnexpired(hash, now);
    }

    public int deleteByHash(String hash) {
        return mapper.deleteByHash(hash);
    }

    public int deleteExpired(long now) {
        return mapper.deleteExpired(now);
    }
}

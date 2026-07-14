package com.example.javastandard.db.repository;

import com.example.javastandard.db.mapper.UserMapper;
import com.example.javastandard.db.model.UserRecord;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    private final UserMapper mapper;

    public UserRepository(UserMapper mapper) {
        this.mapper = mapper;
    }

    public UserRecord findById(String id) {
        return mapper.findById(id);
    }

    public UserRecord findByNormalizedEmail(String email) {
        return mapper.findByNormalizedEmail(email);
    }

    public void insert(UserRecord user) {
        mapper.insert(user);
    }

    public void updateLastLogin(String id, long now) {
        mapper.updateLastLogin(id, now);
    }
}

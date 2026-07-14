package com.example.javastandard.db.mapper;

import com.example.javastandard.db.model.UserRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    UserRecord findById(@Param("id") String id);
    UserRecord findByNormalizedEmail(@Param("email") String email);
    int insert(UserRecord user);
    int updateLastLogin(@Param("id") String id, @Param("updatedAt") long updatedAt);
}

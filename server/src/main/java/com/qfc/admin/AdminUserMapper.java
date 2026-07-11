package com.qfc.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {

    @Select("select id, username, password_hash, display_name, bio, avatar_url, status, created_at, updated_at from admin_user where username = #{username} limit 1")
    AdminUser findByUsername(String username);

    @Select("select id, username, password_hash, display_name, bio, avatar_url, status, created_at, updated_at from admin_user order by id desc")
    List<AdminUser> selectAllUsers();
}

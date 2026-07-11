package com.qfc.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SiteUserMapper extends BaseMapper<SiteUser> {

    @Select("select id, username, password_hash, display_name, bio, avatar_url, status, created_at, updated_at from site_user where username = #{username} limit 1")
    SiteUser findByUsername(String username);

    @Select("select id, username, password_hash, display_name, bio, avatar_url, status, created_at, updated_at from site_user order by id desc")
    List<SiteUser> selectAllUsers();
}

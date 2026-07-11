package com.qfc.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    @Delete("delete from admin_user_role where user_id = #{userId}")
    int deleteByUserId(Long userId);
}

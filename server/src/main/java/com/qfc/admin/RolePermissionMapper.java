package com.qfc.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    @Delete("delete from role_permission where role_id = #{roleId}")
    int deleteByRoleId(Long roleId);
}

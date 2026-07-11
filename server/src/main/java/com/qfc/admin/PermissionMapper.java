package com.qfc.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    @Select("select id, code, name, module, description, created_at, updated_at from permission order by module, id")
    List<Permission> selectAllPermissions();

    @Select({
        "<script>",
        "select id, code, name, module, description, created_at, updated_at from permission",
        "where code in",
        "<foreach collection='codes' item='code' open='(' separator=',' close=')'>#{code}</foreach>",
        "order by module, id",
        "</script>"
    })
    List<Permission> selectByCodes(@Param("codes") List<String> codes);

    @Select("select p.code from permission p inner join role_permission rp on rp.permission_id = p.id where rp.role_id = #{roleId} order by p.module, p.id")
    List<String> selectPermissionCodesByRoleId(Long roleId);

    @Select("select distinct p.code from permission p inner join role_permission rp on rp.permission_id = p.id inner join admin_user_role ur on ur.role_id = rp.role_id where ur.user_id = #{userId} order by p.code")
    List<String> selectPermissionCodesByUserId(Long userId);
}

package com.qfc.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminRoleMapper extends BaseMapper<AdminRole> {

    @Select("select id, code, name, description, built_in, created_at, updated_at from admin_role order by id")
    List<AdminRole> selectAllRoles();

    @Select("select id, code, name, description, built_in, created_at, updated_at from admin_role where code = #{code} limit 1")
    AdminRole findByCode(String code);

    @Select({
        "<script>",
        "select id, code, name, description, built_in, created_at, updated_at from admin_role",
        "where code in",
        "<foreach collection='codes' item='code' open='(' separator=',' close=')'>#{code}</foreach>",
        "order by id",
        "</script>"
    })
    List<AdminRole> selectByCodes(@Param("codes") List<String> codes);

    @Select("select r.code from admin_role r inner join admin_user_role ur on ur.role_id = r.id where ur.user_id = #{userId} order by r.id")
    List<String> selectRoleCodesByUserId(Long userId);
}

package com.qfc.rag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper; import org.apache.ibatis.annotations.*;
@Mapper public interface ProjectMemberMapper extends BaseMapper<ProjectMember> { @Select("select * from project_member where project_id=#{projectId} and user_id=#{userId} and status='ACTIVE' limit 1") ProjectMember find(@Param("projectId") Long p,@Param("userId") Long u); }

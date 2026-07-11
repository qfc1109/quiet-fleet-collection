package com.qfc.project;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    String ACTIVE_COLUMNS = "id, owner_user_id, name, slug, description, cover_url, visibility, sort_order, created_at, updated_at, deleted_at, deleted_by_user_id";

    @Select("select " + ACTIVE_COLUMNS + " from project where visibility = 'PUBLIC' and deleted_at is null order by sort_order desc, created_at desc, id desc")
    List<Project> selectPublicProjects();

    @Select("select " + ACTIVE_COLUMNS + " from project where slug = #{slug} and visibility = 'PUBLIC' and deleted_at is null limit 1")
    Project selectPublicBySlug(String slug);

    @Select("select " + ACTIVE_COLUMNS + " from project where slug = #{slug} and deleted_at is null limit 1")
    Project selectActiveBySlug(String slug);

    @Select("select " + ACTIVE_COLUMNS + " from project where deleted_at is null order by sort_order desc, created_at desc, id desc")
    List<Project> selectAllProjects();

    @Select("select " + ACTIVE_COLUMNS + " from project where owner_user_id = #{ownerUserId} and deleted_at is null order by sort_order desc, created_at desc, id desc")
    List<Project> selectByOwnerUserId(Long ownerUserId);
}

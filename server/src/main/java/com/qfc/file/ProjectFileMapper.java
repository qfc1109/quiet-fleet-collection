package com.qfc.file;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProjectFileMapper extends BaseMapper<ProjectFile> {

    @Select("select id, project_id, original_name, stored_name, file_ext, mime_type, file_size, storage_path, relative_path, preview_type, created_at, updated_at from project_file where project_id = #{projectId} order by relative_path asc, created_at desc, id desc")
    List<ProjectFile> selectByProjectId(Long projectId);

    @Select("select id, project_id, original_name, stored_name, file_ext, mime_type, file_size, storage_path, relative_path, preview_type, created_at, updated_at from project_file where project_id = #{projectId} and relative_path = #{relativePath} order by created_at desc, id desc limit 1")
    ProjectFile selectByProjectIdAndRelativePath(@Param("projectId") Long projectId, @Param("relativePath") String relativePath);
}

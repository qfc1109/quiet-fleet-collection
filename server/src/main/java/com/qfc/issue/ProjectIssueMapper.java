package com.qfc.issue;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProjectIssueMapper extends BaseMapper<ProjectIssue> {

    String VIEW_COLUMNS = "i.id, i.project_id, i.author_user_id, u.username as author_username, "
        + "u.display_name as author_display_name, i.title, i.content, i.status, i.created_at, i.updated_at";

    @Select(
        "select " + VIEW_COLUMNS
            + " from project_issue i"
            + " left join site_user u on u.id = i.author_user_id"
            + " where i.id = #{issueId}"
            + " limit 1"
    )
    ProjectIssueView selectViewById(Long issueId);

    @Select(
        "select " + VIEW_COLUMNS
            + " from project_issue i"
            + " left join site_user u on u.id = i.author_user_id"
            + " where i.project_id = #{projectId}"
            + " order by i.created_at desc, i.id desc"
    )
    List<ProjectIssueView> selectViewsByProjectId(Long projectId);
}

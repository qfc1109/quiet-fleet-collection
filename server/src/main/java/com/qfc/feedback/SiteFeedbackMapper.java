package com.qfc.feedback;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SiteFeedbackMapper extends BaseMapper<SiteFeedback> {

    String VIEW_COLUMNS = "f.id, f.author_user_id, u.username as author_username, "
        + "u.display_name as author_display_name, f.title, f.content, f.status, f.created_at, f.updated_at";

    @Select(
        "select " + VIEW_COLUMNS
            + " from site_feedback f"
            + " left join site_user u on u.id = f.author_user_id"
            + " where f.id = #{feedbackId}"
            + " limit 1"
    )
    SiteFeedbackView selectViewById(Long feedbackId);

    @Select(
        "select " + VIEW_COLUMNS
            + " from site_feedback f"
            + " left join site_user u on u.id = f.author_user_id"
            + " order by f.created_at desc, f.id desc"
    )
    List<SiteFeedbackView> selectAllViews();
}

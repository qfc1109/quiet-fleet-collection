package com.qfc.rag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper; import org.apache.ibatis.annotations.*; import java.util.List;
@Mapper public interface KnowledgeFileMapper extends BaseMapper<KnowledgeFile> { @Select("select * from knowledge_file where project_id=#{p} and file_id=#{f} limit 1") KnowledgeFile find(@Param("p")Long p,@Param("f")Long f); @Select("select * from knowledge_file where project_id=#{p} and included=1 and sensitive=0 and status='AVAILABLE'") List<KnowledgeFile> available(@Param("p")Long p); }

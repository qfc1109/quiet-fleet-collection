package com.qfc.rag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper; import org.apache.ibatis.annotations.*; import java.util.List;
@Mapper public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunk> { @Select("select * from knowledge_chunk where knowledge_file_id=#{id} and (content like concat('%',#{q},'%')) order by chunk_no limit 5") List<KnowledgeChunk> search(@Param("id")Long id,@Param("q")String q); @Delete("delete from knowledge_chunk where knowledge_file_id=#{id}") int deleteByKnowledgeFileId(Long id); }

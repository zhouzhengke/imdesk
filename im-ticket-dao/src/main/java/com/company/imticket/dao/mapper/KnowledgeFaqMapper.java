package com.company.imticket.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.imticket.dao.entity.KnowledgeFaq;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 知识库FAQ表 Mapper
 */
@Mapper
public interface KnowledgeFaqMapper extends BaseMapper<KnowledgeFaq> {

    @Select("SELECT * FROM im_knowledge_faq WHERE enabled = 1 AND deleted = 0 AND MATCH(question, keywords) AGAINST(#{question} IN BOOLEAN MODE) ORDER BY hit_count DESC LIMIT 5")
    List<KnowledgeFaq> searchByQuestion(@Param("question") String question);

    @Update("UPDATE im_knowledge_faq SET hit_count = hit_count + 1 WHERE id = #{id}")
    void incrementHitCount(@Param("id") Long id);
}
package com.oa2.dao;

import com.oa2.pojo.KbDoc;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface KbDocDao {

    //查询所有启用的知识库文档（启动时构建向量索引用）
    @Select("select id, question, answer, keywords, hot, enabled, create_time as createTime " +
            "from day.kb_doc where enabled = 1")
    List<KbDoc> selectAllEnabled();

    //查询热门问题（对话页首屏推荐）
    @Select("select question from day.kb_doc where enabled = 1 and hot = 1 order by id limit 8")
    List<String> selectHotQuestions();
}

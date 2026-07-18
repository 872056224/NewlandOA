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

    //根据关键词搜索知识库
    @Select("select question, answer from day.kb_doc where enabled = 1 and (question like concat('%',#{keyword},'%') or answer like concat('%',#{keyword},'%') or keywords like concat('%',#{keyword},'%')) order by hot desc, id limit 1")
    KbDoc searchByKeyword(String keyword);

    //获取所有启用的问答对（用于DeepSeek上下文）
    @Select("select question, answer from day.kb_doc where enabled = 1 order by id")
    List<KbDoc> selectAllQa();
}

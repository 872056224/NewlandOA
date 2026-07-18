package com.oa7.dao;

import com.oa7.pojo.KbDoc;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface KbDocDao {

    @Select("<script>" +
            "select id, question, answer, keywords, hot, enabled, create_time as createTime " +
            "from day.kb_doc " +
            "<where>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "and (question like concat('%', #{keyword}, '%') " +
            "or answer like concat('%', #{keyword}, '%') " +
            "or keywords like concat('%', #{keyword}, '%'))" +
            "</if>" +
            "</where>" +
            "order by id desc" +
            "</script>")
    List<KbDoc> selectPage(@Param("keyword") String keyword);

    @Select("select id, question, answer, keywords, hot, enabled, create_time as createTime " +
            "from day.kb_doc where id = #{id}")
    KbDoc selectById(@Param("id") Integer id);

    @Insert("insert into day.kb_doc (question, answer, keywords, hot, enabled) " +
            "values (#{question}, #{answer}, #{keywords}, #{hot}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KbDoc doc);

    @Update("update day.kb_doc set question=#{question}, answer=#{answer}, keywords=#{keywords}, " +
            "hot=#{hot}, enabled=#{enabled} where id=#{id}")
    int update(KbDoc doc);

    @Delete("delete from day.kb_doc where id = #{id}")
    int deleteById(@Param("id") Integer id);
}

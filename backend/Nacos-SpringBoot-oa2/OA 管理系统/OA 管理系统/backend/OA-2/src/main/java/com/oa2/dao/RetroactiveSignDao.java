package com.oa2.dao;

import com.oa2.pojo.RetroactiveSign;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface RetroactiveSignDao {

    @Insert("INSERT INTO day.retroactive_sign(number, sign_date, type, reason, status) " +
            "VALUES(#{number}, #{sign_date}, #{type}, #{reason}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RetroactiveSign sign);

    @Select("SELECT * FROM day.retroactive_sign WHERE number=#{number} ORDER BY id DESC LIMIT #{offset}, #{limit}")
    List<RetroactiveSign> selectByNumberPage(@Param("number") int number, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT count(*) FROM day.retroactive_sign WHERE number=#{number}")
    int countByNumber(@Param("number") int number);
}

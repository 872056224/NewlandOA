package com.oa2.dao;

import com.oa2.pojo.Sign;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SignDao {

    @Select("SELECT * FROM day.sign WHERE number = #{number} ORDER BY id DESC")
    List<Sign> selectByNumber(@Param("number") int number);

    @Select("SELECT * FROM day.sign WHERE number = #{number} ORDER BY id DESC LIMIT #{offset}, #{limit}")
    List<Sign> selectByNumberPage(@Param("number") int number,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    @Select("SELECT count(*) FROM day.sign WHERE number = #{number}")
    int countByNumber(@Param("number") int number);

    @Insert("INSERT INTO day.sign (signDate, number, state, type, sign_address) " +
            "VALUES (#{signDate}, #{number}, #{state}, #{type}, #{sign_address})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Sign sign);

    @Select("SELECT * FROM day.sign WHERE number = #{number} AND signDate LIKE #{dateLike} ORDER BY id DESC LIMIT 1")
    Sign selectTodayRecord(@Param("number") int number, @Param("dateLike") String dateLike);

    @Select("SELECT * FROM day.sign WHERE number = #{number} AND signDate >= #{startDate} AND signDate <= #{endDate} ORDER BY id ASC")
    List<Sign> selectByDateRange(@Param("number") int number,
                                 @Param("startDate") String startDate,
                                 @Param("endDate") String endDate);
}

package com.oa7.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * 节假日数据访问层 - day.holiday 表
 */
@Repository
@Mapper
public interface HolidayDao {

    /**
     * 查询指定日期的节假日类型 (WORKDAY/HOLIDAY/REST_DAY)
     */
    @Select("SELECT type FROM day.holiday WHERE date = #{date}")
    String selectHolidayTypeByDate(@Param("date") LocalDate date);
}

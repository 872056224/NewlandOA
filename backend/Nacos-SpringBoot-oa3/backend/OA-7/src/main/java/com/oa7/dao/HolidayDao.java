package com.oa7.dao;

import com.oa7.pojo.Holiday;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

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

    /**
     * 查询指定年份的所有节假日
     */
    @Select("SELECT * FROM day.holiday WHERE year=#{year} ORDER BY date")
    List<Holiday> selectByYear(@Param("year") int year);

    /**
     * 查询指定日期的节假日记录
     */
    @Select("SELECT * FROM day.holiday WHERE date=#{date}")
    Holiday selectByDate(@Param("date") LocalDate date);

    /**
     * 查询指定日期范围内的节假日记录
     */
    @Select("SELECT * FROM day.holiday WHERE date BETWEEN #{start} AND #{end} ORDER BY date")
    List<Holiday> selectByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * 插入或更新节假日记录（存在则更新）
     */
    @Insert("INSERT INTO day.holiday(date, type, description, year) VALUES(#{date}, #{type}, #{description}, #{year}) " +
            "ON DUPLICATE KEY UPDATE type=#{type}, description=#{description}")
    int insertOrUpdate(Holiday holiday);

    /**
     * 更新节假日记录
     */
    @Update("UPDATE day.holiday SET type=#{type}, description=#{description} WHERE date=#{date}")
    int update(Holiday holiday);

    /**
     * 删除指定日期的节假日记录
     */
    @Delete("DELETE FROM day.holiday WHERE date=#{date}")
    int delete(@Param("date") LocalDate date);

    /**
     * 批量插入节假日记录（存在则更新）
     */
    @Insert("<script>" +
            "INSERT INTO day.holiday(date, type, description, year) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.date}, #{item.type}, #{item.description}, #{item.year})" +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE type=VALUES(type), description=VALUES(description)" +
            "</script>")
    int batchInsert(@Param("list") List<Holiday> list);
}

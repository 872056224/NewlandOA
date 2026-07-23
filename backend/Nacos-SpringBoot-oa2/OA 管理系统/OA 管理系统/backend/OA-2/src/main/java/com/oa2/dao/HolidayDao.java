package com.oa2.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Mapper
@Repository
public interface HolidayDao {

    @Select("SELECT type FROM day.holiday WHERE date = #{date}")
    String selectHolidayTypeByDate(LocalDate date);
}

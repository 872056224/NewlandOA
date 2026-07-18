package com.oa7.dao;

import com.oa7.pojo.Duty;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @name: chenle
 * @Date: 2021/12/2 22:44
 * @Author: IAO
 * @Description: ...
 */
@Mapper
@Repository
public interface DutyDao {
    //查询所有职务和人数
    @Select("select duty.duty_id,duty.duty_name,count(emp.duty_id) as duty_num " +
            "from day.duty " +
            "left join day.emp on duty.duty_id = emp.duty_id " +
            "group by duty.duty_id order by duty.duty_id limit #{a},#{b}")
    List<Duty> selectAllDutyAndNum(@Param("a") int a, @Param("b") int b);


    @Select("SELECT d.duty_id, d.duty_name, COUNT(e.number) AS duty_num " +
            "FROM day.duty d " +
            "LEFT JOIN day.emp e ON d.duty_id = e.duty_id " +
            "GROUP BY d.duty_id, d.duty_name " +
            "ORDER BY d.duty_id")
    List<Duty> selectByPageHelper();

    //    统计职务数
    @Select("select count(*) from day.duty")
    int countDuty();

    //更新职务名称
    @Update("update day.duty set duty_name=#{duty_name}  where duty_id=#{duty_id}  ")
    int updateDutyNameById(Duty duty);

    //通过名称查询职务
    @Select("select * from day.duty where duty_name=#{duty_name}  ")
    Duty selectByName(Duty duty);

    //添加职务
    @Insert("insert into day.duty(duty_name) values (#{duty_name}  )")
    int addDuty(Duty duty);
}

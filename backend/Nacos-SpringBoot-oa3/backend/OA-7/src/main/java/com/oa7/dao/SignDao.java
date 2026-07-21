package com.oa7.dao;

import com.oa7.pojo.Emp;
import com.oa7.pojo.Sign;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @name: chenle
 * @Date: 2021/12/1 14:08
 * @Author: IAO
 * @Description: ...
 */
@Repository
@Mapper
public interface SignDao {
    //查询所有出勤记录
    @Select("select * from day.sign order by sign.id")
    List<Sign> selectAll();

    //统计今日未签到任务
    @Select("select count(*) from day.sign where state='未签到' and sign.type='a' and signDate like concat(#{day},'%' ) ")
    int countByDayByStateNo(@Param("day") String day);

    //统计今日已签到人数
    @Select("select count(*) from day.sign where state='已签到' and sign.type='a' and signDate like concat(#{day},'%' ) ")
    int countByDayByStateYes(@Param("day") String day);

    //统计指定日期指定类型的已签到人数
    @Select("select count(*) from day.sign where state='已签到' and sign.type=#{type} and signDate like concat(#{day},'%' ) ")
    int countByDayByStateAndTypeYes(@Param("day") String day, @Param("type") String type);

    //统计指定日期指定类型的未签到人数
    @Select("select count(*) from day.sign where state='未签到' and sign.type=#{type} and signDate like concat(#{day},'%' ) ")
    int countByDayByStateAndTypeNo(@Param("day") String day, @Param("type") String type);

    //查询某个员工考勤信息
//    @Select("select sign.*,dept_name,name from day.sign " +
//            "left join day.emp e on e.number = sign.number " +
//            "left join day.department d on d.dept_id = e.dept_id  " +
//            "where day.sign.number=#{emp.number} " +
//            "order by id limit #{a},#{b} ")
//    List<Sign> selectByPage(@Param("emp") Emp emp , @Param("a") int a , @Param("b") int b);

    @Select("select sign.*,dept_name,name from day.sign " +
            "left join day.emp e on e.number = sign.number " +
            "left join day.department d on d.dept_id = e.dept_id  " +
            "where day.sign.number=#{emp.number} " +
            "order by id")
    List<Sign> selectByPage(@Param("emp") Emp emp);

    //查询已签到的员工考勤信息
    @Select("select sign.*,dept_name,name from day.sign " +
            "left join day.emp e on e.number = sign.number " +
            "left join day.department d on d.dept_id = e.dept_id  " +
            "where day.sign.state='已签到' " +
            "order by id DESC limit #{a},#{b} ")
    List<Sign> selectYesByPage(@Param("a") int a , @Param("b") int b);



    //查询未签到的员工考勤信息
    @Select("select sign.*,dept_name,name from day.sign " +
            "left join day.emp e on e.number = sign.number " +
            "left join day.department d on d.dept_id = e.dept_id  " +
            "where day.sign.state='未签到' " +
            "order by id DESC limit #{a},#{b} ")
    List<Sign> selectNoByPage(@Param("a") int a , @Param("b") int b);

    //查询所有员工的考勤信息
    @Select("select sign.*,dept_name,name from day.sign " +
            "left join day.emp e on e.number = sign.number " +
            "left join day.department d on d.dept_id = e.dept_id  " +
            "order by id limit #{a},#{b} ")
    List<Sign> selectAllByPage(@Param("a") int a , @Param("b") int b);

    //查询今日已签到的员工的考勤信息
    @Select("select sign.*,dept_name,name from day.sign " +
            "left join day.emp e on e.number = sign.number " +
            "left join day.department d on d.dept_id = e.dept_id  " +
            "where day.sign.signDate like concat(#{today},'%' )" +
            "and day.sign.state='已签到'" +
            "order by id limit #{a},#{b} ")
    List<Sign> selectToDayYesByPage(@Param("a") int a , @Param("b") int b , @Param("today") String today);

    //查询今日未签到的员工的考勤信息
    @Select("select sign.*,dept_name,name from day.sign " +
            "left join day.emp e on e.number = sign.number " +
            "left join day.department d on d.dept_id = e.dept_id  " +
            "where day.sign.signDate like concat(#{today},'%' )" +
            "and day.sign.state='未签到'" +
            "order by id limit #{a},#{b} ")
    List<Sign> selectToDayNoByPage(@Param("a") int a , @Param("b") int b , @Param("today") String today);

    //统计所有员工考勤数
    @Select("select count(*) from day.sign")
    int countUser();

    //统计某个员工的考勤数
    @Select("select count(*) from day.sign where number=#{number} ")
    int countSignByNumber(Emp emp);

    //统计已签到的员工数
    @Select("select count(*) from day.sign where state='已签到'")
    int countUserYes();

    //统计未签到的员工数
    @Select("select count(*) from day.sign where state='未签到'")
    int countUserNo();

    //统计今日已签到的员工数
    @Select("select count(*) from day.sign where state='已签到' and day.sign.signDate like concat(#{today},'%' ) ")
    int countToDayYes(@Param("today") String today);

    //统计今日未签到的员工数
    @Select("select count(*) from day.sign where state='未签到' and day.sign.signDate like concat(#{today},'%' ) ")
    int countToDayNo(@Param("today") String today);

    //查询某个员工某天的考勤信息
    @Select("select sign.*,dept_name,name from day.sign " +
            "left join day.emp e on e.number = sign.number " +
            "left join day.department d on d.dept_id = e.dept_id " +
            "where day.sign.number=#{emp.number}   " +
            "and signDate like concat(#{today},'%' )")
    List<Sign> selectEmpSign(@Param("emp") Emp emp , @Param("today") String today);

    //    添加考勤任务
    @Insert("insert into day.sign(signDate,number,state,type) values (#{signDate} ,#{number} ,#{state},#{type}  )")
    int addSign(Sign sign);

    //    更新考勤状态
    @Update("update day.sign set state=#{sign.state}  ,signDate=#{date}  where number=#{sign.number}  and signDate=#{sign.signDate}   ")
    int updateState(@Param("sign") Sign sign , @Param("date") String date);

    // 补签专用：按员工编号+日期+类型更新签到状态
    @Update("UPDATE day.sign SET state='已签到' WHERE number=#{number} AND signDate LIKE CONCAT(#{date}, '%') AND type=#{type}")
    int updateStateByDateAndType(@Param("number") int number, @Param("date") String date, @Param("type") String type);

    @Delete("DELETE  from day.sign  WHERE signDate  BETWEEN  signDate=#{beganDate} and signDate=#{endDate}")
    void deleteSign(String beganDate,String endDate);
}

package com.oa7.dao;

import com.oa7.pojo.Admin;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface AdmDao {

    @Select("select * from day.admin where name=#{name} ")
    Admin selectByName(Admin admin);

    @Insert("insert into day.admin (name, pwd) VALUES (#{name} ,#{pwd} )")
    int insertAdm(Admin admin);

    @Select("select id from day.admin")
    List<Integer> selectAllIds();

    /**
     * 联查 admin + emp + department 获取完整信息
     */
    @Select("SELECT a.id, a.name, a.pwd, a.emp_number AS empNumber, " +
            "e.name AS empName, e.dept_id AS deptId, e.duty_id AS dutyId, " +
            "d.dept_name AS deptName " +
            "FROM day.admin a " +
            "LEFT JOIN day.emp e ON e.number = a.emp_number " +
            "LEFT JOIN day.department d ON d.dept_id = e.dept_id " +
            "WHERE a.name = #{name}")
    Admin selectAdminWithEmpByName(@Param("name") String name);

    /**
     * 根据 ID 联查 admin + emp
     */
    @Select("SELECT a.id, a.name, a.pwd, a.emp_number AS empNumber, " +
            "e.name AS empName, e.dept_id AS deptId, e.duty_id AS dutyId, " +
            "d.dept_name AS deptName " +
            "FROM day.admin a " +
            "LEFT JOIN day.emp e ON e.number = a.emp_number " +
            "LEFT JOIN day.department d ON d.dept_id = e.dept_id " +
            "WHERE a.id = #{id}")
    Admin selectAdminWithEmpById(@Param("id") int id);

    /**
     * 获取所有有资格接收通知的管理员
     * （已绑定员工且职务为部长/副部长/董事长）
     */
    @Select("SELECT a.id AS adminId, a.name AS adminName, a.emp_number AS targetNumber, " +
            "e.dept_id AS deptId, e.duty_id AS dutyId " +
            "FROM day.admin a " +
            "INNER JOIN day.emp e ON e.number = a.emp_number " +
            "WHERE a.emp_number IS NOT NULL " +
            "AND e.duty_id IN (1, 2, 17)")
    List<Map<String, Object>> selectAllEligibleAdmins();
}

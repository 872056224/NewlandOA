package com.oa7.dao;

import com.oa7.pojo.Department;
import com.oa7.pojo.Emp;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @name: chenle
 * @Date: 2021/12/2 20:05
 * @Author: IAO
 * @Description: ...
 */
@Mapper
@Repository
public interface DeptDao {
    //查询所有部门和人数
    @Select("select department.dept_id,department.dept_name,count(emp.dept_id) as dept_num " +
            "from day.department " +
            "left join day.emp on department.dept_id = emp.dept_id " +
            "group by department.dept_id order by department.dept_id limit #{a},#{b}")
    List<Department> selectAllDeptAndNum(@Param("a") int a, @Param("b") int b);


    @Select("SELECT d.dept_id, d.dept_name, COUNT(e.number) AS dept_num " +
            "FROM day.department d " +
            "LEFT JOIN day.emp e ON d.dept_id = e.dept_id " +
            "GROUP BY d.dept_id, d.dept_name " +
            "ORDER BY d.dept_id")
    List<Department> selectByPageHelper();


    // 统计部门数
    @Select("select count(*) from day.department")
    int countDept();

    //更新部门信息
    @Update("update day.department set dept_name=#{dept_name} where dept_id=#{dept_id} ")
    int updateDeptNameById(Department department);

    //通过部门名字查询部门
    @Select("select * from day.department where dept_name=#{dept_name} ")
    Department selectByName(Department department);

    //添加新的部门
    @Insert("insert into day.department(dept_name) values (#{dept_name} )")
    int addDept(Department department);
}

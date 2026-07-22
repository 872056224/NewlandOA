package com.oa7.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.oa7.dao.DeptDao;
import com.oa7.pojo.Department;
import com.oa7.service.DeptService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeptServiceImpl implements DeptService {

    @Autowired
    private DeptDao deptDao;

    @Override
    public RESP selectByPage(int currentPage, int pageSize) {
        try {
            PageHelper.startPage(currentPage, pageSize);
            List<Department> list = deptDao.selectByPageHelper();
            PageInfo<Department> pageInfo = new PageInfo<>(list);
            return RESP.ok(pageInfo.getList(), pageInfo.getPageNum(), (int) pageInfo.getTotal());
        } catch (Exception e) {
            e.printStackTrace();
            // 降级：直接 LIMIT 查询
            int offset = (currentPage - 1) * pageSize;
            List<Department> list = deptDao.selectAllDeptAndNum(offset, pageSize);
            int total = deptDao.countDept();
            return RESP.ok(list, currentPage, total);
        }
    }

    @Override
    public RESP add(Department dept, int currentPage, int pageSize) {
        deptDao.addDept(dept);
        return selectByPage(currentPage, pageSize);
    }

    @Override
    public RESP update(int deptId, Department dept, int currentPage, int pageSize) {
        dept.setDept_id(deptId);
        deptDao.updateDeptNameById(dept);
        return selectByPage(currentPage, pageSize);
    }
}

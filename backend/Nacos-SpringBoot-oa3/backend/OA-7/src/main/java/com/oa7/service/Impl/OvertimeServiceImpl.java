package com.oa7.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.oa7.dao.EmpDao;
import com.oa7.dao.NotificationDao;
import com.oa7.dao.OvertimeDao;
import com.oa7.pojo.Admin;
import com.oa7.pojo.Emp;
import com.oa7.pojo.OvertimeRequest;
import com.oa7.service.OvertimeService;
import com.oa7.util.AdminAuthUtil;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;

@Service
public class OvertimeServiceImpl implements OvertimeService {

    @Autowired
    private OvertimeDao overtimeDao;

    @Autowired
    private EmpDao empDao;

    @Autowired
    private NotificationDao notificationDao;

    @Override
    public RESP getPending(int currentPage, int pageSize, HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);
        PageHelper.startPage(currentPage, pageSize);
        List<OvertimeRequest> list;
        if (admin != null && admin.isDeptHead()) {
            list = overtimeDao.selectPendingByDept(admin.getDeptId());
        } else {
            list = overtimeDao.selectPending();
        }
        PageInfo<OvertimeRequest> pageInfo = new PageInfo<>(list);
        return RESP.ok(list, pageInfo.getPageNum(), (int) pageInfo.getTotal());
    }

    @Override
    public RESP approve(int id, BigDecimal actualHours, HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);
        if (admin == null) return RESP.error(401, "未登录");

        OvertimeRequest req = overtimeDao.selectById(id);
        if (req == null) return RESP.error("加班申请不存在");
        if (!"PENDING".equals(req.getStatus())) {
            return RESP.error("该申请已被他人处理");
        }

        if (admin.isDeptHead()) {
            Integer deptId = getApplicantDeptId(req.getEmpId());
            if (deptId == null || !deptId.equals(admin.getDeptId())) {
                return RESP.error(403, "无权审批其他部门的加班申请");
            }
        }

        BigDecimal finalHours = actualHours != null && actualHours.compareTo(BigDecimal.ZERO) > 0
                ? actualHours : req.getTotalHours();
        req.setStatus("APPROVED");
        req.setActualHours(finalHours);
        int ret = overtimeDao.updateStatusWithVersion(req);
        if (ret == 0) return RESP.error("该申请已被他人处理，请刷新后重试");

        notificationDao.insert("overtime_approved", "加班已批准",
                "您在 " + req.getOvertimeDate() + " 的加班申请已获批准（核定 " + finalHours + " 小时）",
                req.getEmpId(), String.valueOf(id));
        notificationDao.markAllReadByBizId(String.valueOf(id));
        return RESP.ok("操作成功");
    }

    @Override
    public RESP reject(int id, String reason, HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);
        if (admin == null) return RESP.error(401, "未登录");

        OvertimeRequest req = overtimeDao.selectById(id);
        if (req == null) return RESP.error("加班申请不存在");
        if (!"PENDING".equals(req.getStatus())) {
            return RESP.error("该申请已被他人处理");
        }

        if (admin.isDeptHead()) {
            Integer deptId = getApplicantDeptId(req.getEmpId());
            if (deptId == null || !deptId.equals(admin.getDeptId())) {
                return RESP.error(403, "无权拒绝其他部门的加班申请");
            }
        }

        req.setStatus("REJECTED");
        req.setRejectReason(reason != null ? reason : "");
        int ret = overtimeDao.updateStatusWithVersion(req);
        if (ret == 0) return RESP.error("该申请已被他人处理，请刷新后重试");

        notificationDao.insert("overtime_rejected", "加班已拒绝",
                "您在 " + req.getOvertimeDate() + " 的加班申请已被拒绝" +
                        (reason != null && !reason.isEmpty() ? "（原因：" + reason + "）" : ""),
                req.getEmpId(), String.valueOf(id));
        notificationDao.markAllReadByBizId(String.valueOf(id));
        return RESP.ok("操作成功");
    }

    private Integer getApplicantDeptId(int empId) {
        Emp emp = empDao.selectByEmpNumber(empId);
        return emp != null ? emp.getDept_id() : null;
    }
}

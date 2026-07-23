package com.oa7.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.oa7.constant.TodayStatus;
import com.oa7.dao.AttendanceDao;
import com.oa7.dao.NotificationDao;
import com.oa7.dao.RetroactiveSignDao;
import com.oa7.dao.SignDao;
import com.oa7.pojo.Admin;
import com.oa7.pojo.Attendance;
import com.oa7.pojo.RetroactiveSign;
import com.oa7.service.RecalculateAttendanceService;
import com.oa7.service.RetroactiveSignService;
import com.oa7.util.AdminAuthUtil;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class RetroactiveSignServiceImpl implements RetroactiveSignService {

    @Autowired
    private RetroactiveSignDao retroactiveSignDao;

    @Autowired
    private SignDao signDao;

    @Autowired
    private NotificationDao notificationDao;

    @Autowired
    private AttendanceDao attendanceDao;

    @Autowired
    private RecalculateAttendanceService recalculateAttendanceService;

    @Override
    public RESP getPending(int currentPage, int pageSize, HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);

        PageHelper.startPage(currentPage, pageSize);
        List<RetroactiveSign> list;

        if (admin != null && admin.isDeptHead()) {
            // DEPT_HEAD：只看到本部门员工的补签申请
            list = retroactiveSignDao.selectPendingByDept(admin.getDeptId());
        } else {
            // CHAIRMAN / HR_DIRECTOR：全部
            list = retroactiveSignDao.selectPending();
        }

        PageInfo<RetroactiveSign> pageInfo = new PageInfo<>(list);
        return RESP.ok(list, pageInfo.getPageNum(), (int) pageInfo.getTotal());
    }

    @Override
    public RESP approve(int id, HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);
        if (admin == null) return RESP.error(401, "未登录");

        RetroactiveSign sign = retroactiveSignDao.selectById(id);
        if (sign == null) return RESP.error("补签申请不存在");
        if (!"待审批".equals(sign.getStatus())) {
            return RESP.error("该申请已被他人处理，当前状态：" + sign.getStatus());
        }

        // DEPT_HEAD：只能审批本部门的补签
        if (admin.isDeptHead()) {
            Integer deptId = retroactiveSignDao.selectDeptIdBySignId(id);
            if (deptId == null || !deptId.equals(admin.getDeptId())) {
                return RESP.error(403, "无权审批其他部门的补签申请");
            }
        }

        // 乐观锁更新
        int ret = retroactiveSignDao.updateStatusWithVersion(id, "已批准", sign.getVersion());
        if (ret == 0) {
            return RESP.error("该申请已被他人处理，请刷新后重试");
        }

        // 更新签到状态
        sign = retroactiveSignDao.selectById(id);
        if (sign != null) {
            signDao.updateStateByDateAndType(sign.getNumber(), sign.getSign_date(), sign.getType());
            notificationDao.insert("retroactive_approved", "补签已批准",
                    "您在 " + sign.getSign_date() + " 的补签申请已获批准",
                    sign.getNumber(), String.valueOf(id));

            notificationDao.markAllReadByBizId(String.valueOf(id));

            try {
                LocalDate signDate = LocalDate.parse(sign.getSign_date());
                Integer empId = sign.getNumber();

                Attendance attendance = attendanceDao.selectByEmpAndDate(empId, signDate);
                if (attendance == null) {
                    attendanceDao.insertOrUpdate(empId, signDate, TodayStatus.NOT_CHECKED_IN);
                    attendance = new Attendance();
                    attendance.setEmpId(empId);
                    attendance.setDate(signDate);
                }

                attendance.setCheckInTime(LocalDateTime.of(signDate, LocalTime.of(9, 0)));
                attendance.setCheckOutTime(LocalDateTime.of(signDate, LocalTime.of(18, 0)));
                attendance.setTodayStatus(TodayStatus.CHECKED_OUT);

                attendanceDao.updateCheckTime(attendance);
                attendanceDao.updateTodayStatusByEmpAndDate(empId, signDate, TodayStatus.CHECKED_OUT);
                recalculateAttendanceService.recalculate(empId, signDate);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return RESP.ok("操作成功");
    }

    @Override
    public RESP reject(int id, HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);
        if (admin == null) return RESP.error(401, "未登录");

        RetroactiveSign sign = retroactiveSignDao.selectById(id);
        if (sign == null) return RESP.error("补签申请不存在");
        if (!"待审批".equals(sign.getStatus())) {
            return RESP.error("该申请已被他人处理，当前状态：" + sign.getStatus());
        }

        // DEPT_HEAD：只能拒绝本部门的补签
        if (admin.isDeptHead()) {
            Integer deptId = retroactiveSignDao.selectDeptIdBySignId(id);
            if (deptId == null || !deptId.equals(admin.getDeptId())) {
                return RESP.error(403, "无权拒绝其他部门的补签申请");
            }
        }

        int ret = retroactiveSignDao.updateStatusWithVersion(id, "已拒绝", sign.getVersion());
        if (ret == 0) {
            return RESP.error("该申请已被他人处理，请刷新后重试");
        }

        sign = retroactiveSignDao.selectById(id);
        if (sign != null) {
            notificationDao.insert("retroactive_rejected", "补签已拒绝",
                    "您在 " + sign.getSign_date() + " 的补签申请已被拒绝",
                    sign.getNumber(), String.valueOf(id));

            notificationDao.markAllReadByBizId(String.valueOf(id));
        }

        return RESP.ok("操作成功");
    }

    @Override
    public RESP revoke(int id, HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);
        if (admin == null) return RESP.error(401, "未登录");

        RetroactiveSign sign = retroactiveSignDao.selectById(id);
        if (sign == null) return RESP.error("补签申请不存在");
        if (!"已批准".equals(sign.getStatus())) {
            return RESP.error("该申请已被他人处理，当前状态：" + sign.getStatus());
        }

        // DEPT_HEAD：只能撤销本部门的补签
        if (admin.isDeptHead()) {
            Integer deptId = retroactiveSignDao.selectDeptIdBySignId(id);
            if (deptId == null || !deptId.equals(admin.getDeptId())) {
                return RESP.error(403, "无权撤销其他部门的补签申请");
            }
        }

        int ret = retroactiveSignDao.updateStatusWithVersion(id, "待审批", sign.getVersion());
        if (ret == 0) {
            return RESP.error("该申请已被他人处理，请刷新后重试");
        }

        sign = retroactiveSignDao.selectById(id);
        if (sign != null) {
            try {
                LocalDate signDate = LocalDate.parse(sign.getSign_date());
                Integer empId = sign.getNumber();

                Attendance attendance = attendanceDao.selectByEmpAndDate(empId, signDate);
                if (attendance != null) {
                    attendance.setCheckInTime(null);
                    attendance.setCheckOutTime(null);
                    attendanceDao.updateCheckTime(attendance);
                    recalculateAttendanceService.recalculate(empId, signDate);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            String typeLabel = sign.getType().equals("a") ? "上午" : "下午";
            notificationDao.insert("retroactive_revoked", "补签已撤销",
                    "您在 " + sign.getSign_date() + "(" + typeLabel + ") 的补签申请已被撤销",
                    sign.getNumber(), String.valueOf(id));

            notificationDao.markAllReadByBizId(String.valueOf(id));
        }

        return RESP.ok("操作成功");
    }
}

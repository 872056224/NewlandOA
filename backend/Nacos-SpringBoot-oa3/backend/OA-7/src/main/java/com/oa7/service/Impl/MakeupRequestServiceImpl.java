package com.oa7.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.oa7.constant.TodayStatus;
import com.oa7.dao.AttendanceDao;
import com.oa7.dao.MakeupRequestDao;
import com.oa7.dao.NotificationDao;
import com.oa7.pojo.Attendance;
import com.oa7.pojo.MakeupRequest;
import com.oa7.service.MakeupRequestService;
import com.oa7.service.RecalculateAttendanceService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class MakeupRequestServiceImpl implements MakeupRequestService {

    @Autowired
    private MakeupRequestDao makeupRequestDao;

    @Autowired
    private NotificationDao notificationDao;

    @Autowired
    private AttendanceDao attendanceDao;

    @Autowired
    private RecalculateAttendanceService recalculateAttendanceService;

    @Override
    public RESP getPending(int currentPage, int pageSize) {
        PageHelper.startPage(currentPage, pageSize);
        List<MakeupRequest> list = makeupRequestDao.selectPending();
        PageInfo<MakeupRequest> pageInfo = new PageInfo<>(list);
        return RESP.ok(list, pageInfo.getPageNum(), (int) pageInfo.getTotal());
    }

    @Override
    public RESP approve(int id) {
        MakeupRequest request = makeupRequestDao.selectById(id);
        if (request == null) return RESP.error("补卡申请不存在");
        if (!"PENDING".equals(request.getStatus())) {
            return RESP.error("该申请已被他人处理，当前状态：" + request.getStatus());
        }

        // 乐观锁更新
        int ret = makeupRequestDao.updateStatusWithVersion(id, "APPROVED", request.getVersion());
        if (ret == 0) {
            return RESP.error("该申请已被他人处理，请刷新后重试");
        }

        // 解析日期和类型
        LocalDate date = LocalDate.parse(request.getDate());
        String type = request.getType(); // CHECK_IN / CHECK_OUT
        Integer empId = request.getEmpId();

        // 查找或创建考勤记录
        Attendance attendance = attendanceDao.selectByEmpAndDate(empId, date);
        if (attendance == null) {
            attendanceDao.insertOrUpdate(empId, date, TodayStatus.NOT_CHECKED_IN);
            attendance = new Attendance();
            attendance.setEmpId(empId);
            attendance.setDate(date);
        }

        // 根据补卡类型设置签到/签退时间
        if ("CHECK_IN".equals(type)) {
            attendance.setCheckInTime(LocalDateTime.of(date, LocalTime.of(9, 0)));
        } else if ("CHECK_OUT".equals(type)) {
            attendance.setCheckOutTime(LocalDateTime.of(date, LocalTime.of(18, 0)));
        }

        // 更新考勤记录
        attendanceDao.updateCheckTime(attendance);

        // 考勤重算
        recalculateAttendanceService.recalculate(empId, date);

        // 通知员工
        String typeLabel = "CHECK_IN".equals(type) ? "上班卡" : "下班卡";
        notificationDao.insert("makeup_approved", "补卡已批准",
                "您在 " + request.getDate() + " 的" + typeLabel + "补卡申请已获批准",
                empId, String.valueOf(id));

        // 将所有管理员通知标记为已读
        notificationDao.markAllReadByBizId(String.valueOf(id));

        return RESP.ok("操作成功");
    }

    @Override
    public RESP reject(int id) {
        MakeupRequest request = makeupRequestDao.selectById(id);
        if (request == null) return RESP.error("补卡申请不存在");
        if (!"PENDING".equals(request.getStatus())) {
            return RESP.error("该申请已被他人处理，当前状态：" + request.getStatus());
        }

        // 乐观锁更新
        int ret = makeupRequestDao.updateStatusWithVersion(id, "REJECTED", request.getVersion());
        if (ret == 0) {
            return RESP.error("该申请已被他人处理，请刷新后重试");
        }

        // 通知员工
        String typeLabel = "CHECK_IN".equals(request.getType()) ? "上班卡" : "下班卡";
        notificationDao.insert("makeup_rejected", "补卡已拒绝",
                "您在 " + request.getDate() + " 的" + typeLabel + "补卡申请已被拒绝",
                request.getEmpId(), String.valueOf(id));

        // 将所有管理员通知标记为已读
        notificationDao.markAllReadByBizId(String.valueOf(id));

        return RESP.ok("操作成功");
    }

    @Override
    public RESP revoke(int id) {
        MakeupRequest request = makeupRequestDao.selectById(id);
        if (request == null) return RESP.error("补卡申请不存在");
        if (!"APPROVED".equals(request.getStatus())) {
            return RESP.error("该申请已被他人处理，当前状态：" + request.getStatus());
        }

        // 乐观锁更新
        int ret = makeupRequestDao.updateStatusWithVersion(id, "PENDING", request.getVersion());
        if (ret == 0) {
            return RESP.error("该申请已被他人处理，请刷新后重试");
        }

        // 清除之前设置的签到/签退时间
        LocalDate date = LocalDate.parse(request.getDate());
        String type = request.getType();
        Integer empId = request.getEmpId();

        Attendance attendance = attendanceDao.selectByEmpAndDate(empId, date);
        if (attendance != null) {
            if ("CHECK_IN".equals(type)) {
                attendance.setCheckInTime(null);
            } else if ("CHECK_OUT".equals(type)) {
                attendance.setCheckOutTime(null);
            }
            attendanceDao.updateCheckTime(attendance);
            recalculateAttendanceService.recalculate(empId, date);
        }

        // 通知员工
        String typeLabel = "CHECK_IN".equals(type) ? "上班卡" : "下班卡";
        notificationDao.insert("makeup_revoked", "补卡已撤销",
                "您在 " + request.getDate() + " 的" + typeLabel + "补卡申请已被撤销",
                empId, String.valueOf(id));

        // 将所有管理员通知标记为已读
        notificationDao.markAllReadByBizId(String.valueOf(id));

        return RESP.ok("操作成功");
    }
}

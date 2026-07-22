package com.oa7.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.oa7.constant.TodayStatus;
import com.oa7.dao.AttendanceDao;
import com.oa7.dao.NotificationDao;
import com.oa7.dao.RetroactiveSignDao;
import com.oa7.dao.SignDao;
import com.oa7.pojo.Attendance;
import com.oa7.pojo.RetroactiveSign;
import com.oa7.service.RecalculateAttendanceService;
import com.oa7.service.RetroactiveSignService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public RESP getPending(int currentPage, int pageSize) {
        PageHelper.startPage(currentPage, pageSize);
        List<RetroactiveSign> list = retroactiveSignDao.selectPending();
        PageInfo<RetroactiveSign> pageInfo = new PageInfo<>(list);
        return RESP.ok(list, pageInfo.getPageNum(), (int) pageInfo.getTotal());
    }

    @Override
    public RESP approve(int id) {
        RetroactiveSign sign = retroactiveSignDao.selectById(id);
        if (sign == null) return RESP.error("补签申请不存在");
        if (!"待审批".equals(sign.getStatus())) {
            return RESP.error("该申请已被他人处理，当前状态：" + sign.getStatus());
        }

        // 乐观锁更新
        int ret = retroactiveSignDao.updateStatusWithVersion(id, "已批准", sign.getVersion());
        if (ret == 0) {
            return RESP.error("该申请已被他人处理，请刷新后重试");
        }

        // 更新签到状态
        sign = retroactiveSignDao.selectById(id); // 重新读取最新数据
        if (sign != null) {
            signDao.updateStateByDateAndType(sign.getNumber(), sign.getSign_date(), sign.getType());
            String typeLabel = sign.getType().equals("a") ? "上午" : "下午";
            notificationDao.insert("retroactive_approved", "补签已批准",
                    "您在 " + sign.getSign_date() + "(" + typeLabel + ") 的补签申请已获批准",
                    sign.getNumber(), String.valueOf(id));

            // 将该补签单的所有管理员通知标记为已读
            notificationDao.markAllReadByBizId(String.valueOf(id));

            // 更新考勤记录并触发重算
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

                if ("a".equals(sign.getType())) {
                    attendance.setCheckInTime(LocalDateTime.of(signDate, LocalTime.of(9, 0)));
                } else if ("p".equals(sign.getType())) {
                    attendance.setCheckOutTime(LocalDateTime.of(signDate, LocalTime.of(18, 0)));
                }

                attendanceDao.updateCheckTime(attendance);
                recalculateAttendanceService.recalculate(empId, signDate);
            } catch (Exception e) {
                // 考勤更新失败不应阻塞审批流程，仅记录日志
                e.printStackTrace();
            }
        }

        return RESP.ok("操作成功");
    }

    @Override
    public RESP reject(int id) {
        RetroactiveSign sign = retroactiveSignDao.selectById(id);
        if (sign == null) return RESP.error("补签申请不存在");
        if (!"待审批".equals(sign.getStatus())) {
            return RESP.error("该申请已被他人处理，当前状态：" + sign.getStatus());
        }

        // 乐观锁更新
        int ret = retroactiveSignDao.updateStatusWithVersion(id, "已拒绝", sign.getVersion());
        if (ret == 0) {
            return RESP.error("该申请已被他人处理，请刷新后重试");
        }

        sign = retroactiveSignDao.selectById(id);
        if (sign != null) {
            String typeLabel = sign.getType().equals("a") ? "上午" : "下午";
            notificationDao.insert("retroactive_rejected", "补签已拒绝",
                    "您在 " + sign.getSign_date() + "(" + typeLabel + ") 的补签申请已被拒绝",
                    sign.getNumber(), String.valueOf(id));

            // 将该补签单的所有管理员通知标记为已读
            notificationDao.markAllReadByBizId(String.valueOf(id));
        }

        return RESP.ok("操作成功");
    }

    @Override
    public RESP revoke(int id) {
        RetroactiveSign sign = retroactiveSignDao.selectById(id);
        if (sign == null) return RESP.error("补签申请不存在");
        if (!"已批准".equals(sign.getStatus())) {
            return RESP.error("该申请已被他人处理，当前状态：" + sign.getStatus());
        }

        // 乐观锁更新
        int ret = retroactiveSignDao.updateStatusWithVersion(id, "待审批", sign.getVersion());
        if (ret == 0) {
            return RESP.error("该申请已被他人处理，请刷新后重试");
        }

        sign = retroactiveSignDao.selectById(id);
        if (sign != null) {
            // 清除之前设置的签到/签退时间
            try {
                LocalDate signDate = LocalDate.parse(sign.getSign_date());
                Integer empId = sign.getNumber();

                Attendance attendance = attendanceDao.selectByEmpAndDate(empId, signDate);
                if (attendance != null) {
                    if ("a".equals(sign.getType())) {
                        attendance.setCheckInTime(null);
                    } else if ("p".equals(sign.getType())) {
                        attendance.setCheckOutTime(null);
                    }
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

            // 将该补签单的所有管理员通知标记为已读
            notificationDao.markAllReadByBizId(String.valueOf(id));
        }

        return RESP.ok("操作成功");
    }
}

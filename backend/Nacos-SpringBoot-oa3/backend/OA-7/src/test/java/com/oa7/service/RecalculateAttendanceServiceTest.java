package com.oa7.service;

import com.oa7.constant.AttendanceStatus;
import com.oa7.constant.HolidayType;
import com.oa7.constant.TodayStatus;
import com.oa7.dao.AttendanceDao;
import com.oa7.dao.HolidayDao;
import com.oa7.dao.LeaveDao;
import com.oa7.pojo.Attendance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 考勤重算服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class RecalculateAttendanceServiceTest {

    @Mock
    private AttendanceDao attendanceDao;

    @Mock
    private HolidayDao holidayDao;

    @Mock
    private LeaveDao leaveDao;

    @InjectMocks
    private RecalculateAttendanceService service;

    private static final int EMP_ID = 1001;
    private static final LocalDate DATE = LocalDate.of(2024, 3, 15);

    private Attendance buildAttendance(TodayStatus todayStatus,
                                       LocalDateTime checkIn, LocalDateTime checkOut) {
        Attendance att = new Attendance();
        att.setId(1L);
        att.setEmpId(EMP_ID);
        att.setDate(DATE);
        att.setTodayStatus(todayStatus);
        att.setCheckInTime(checkIn);
        att.setCheckOutTime(checkOut);
        return att;
    }

    @BeforeEach
    void setUp() {
        // Default: no holiday, no leave (lenient — some tests override these)
        lenient().when(holidayDao.selectHolidayTypeByDate(any())).thenReturn(null);
        lenient().when(leaveDao.countApprovedLeaveToday(anyInt(), anyString())).thenReturn(0);
    }

    @Test
    void testRecalculate_NoAttendanceRecord() {
        when(attendanceDao.selectByEmpAndDate(EMP_ID, DATE)).thenReturn(null);

        AttendanceStatus result = service.recalculate(EMP_ID, DATE);

        assertNull(result);
        verify(attendanceDao, never()).updateAttendanceStatus(anyLong(), any());
    }

    @Test
    void testRecalculate_Holiday() {
        Attendance att = buildAttendance(TodayStatus.NOT_CHECKED_IN, null, null);
        when(attendanceDao.selectByEmpAndDate(EMP_ID, DATE)).thenReturn(att);
        when(holidayDao.selectHolidayTypeByDate(DATE)).thenReturn("HOLIDAY");

        AttendanceStatus result = service.recalculate(EMP_ID, DATE);

        assertEquals(AttendanceStatus.HOLIDAY, result);
        verify(attendanceDao).updateAttendanceStatus(1L, AttendanceStatus.HOLIDAY);
    }

    @Test
    void testRecalculate_RestDay() {
        Attendance att = buildAttendance(TodayStatus.NOT_CHECKED_IN, null, null);
        when(attendanceDao.selectByEmpAndDate(EMP_ID, DATE)).thenReturn(att);
        when(holidayDao.selectHolidayTypeByDate(DATE)).thenReturn("REST_DAY");

        AttendanceStatus result = service.recalculate(EMP_ID, DATE);

        assertEquals(AttendanceStatus.REST_DAY, result);
        verify(attendanceDao).updateAttendanceStatus(1L, AttendanceStatus.REST_DAY);
    }

    @Test
    void testRecalculate_LeaveByTodayStatus() {
        Attendance att = buildAttendance(TodayStatus.LEAVE, null, null);
        when(attendanceDao.selectByEmpAndDate(EMP_ID, DATE)).thenReturn(att);

        AttendanceStatus result = service.recalculate(EMP_ID, DATE);

        assertEquals(AttendanceStatus.LEAVE, result);
    }

    @Test
    void testRecalculate_LeaveByApprovedLeave() {
        Attendance att = buildAttendance(TodayStatus.NOT_CHECKED_IN, null, null);
        when(attendanceDao.selectByEmpAndDate(EMP_ID, DATE)).thenReturn(att);
        when(leaveDao.countApprovedLeaveToday(EMP_ID, DATE.toString())).thenReturn(1);

        AttendanceStatus result = service.recalculate(EMP_ID, DATE);

        assertEquals(AttendanceStatus.LEAVE, result);
    }

    @Test
    void testRecalculate_DayOff() {
        Attendance att = buildAttendance(TodayStatus.DAY_OFF, null, null);
        when(attendanceDao.selectByEmpAndDate(EMP_ID, DATE)).thenReturn(att);

        AttendanceStatus result = service.recalculate(EMP_ID, DATE);

        assertEquals(AttendanceStatus.DAY_OFF, result);
    }

    @Test
    void testRecalculate_Normal() {
        LocalDateTime checkIn = LocalDateTime.of(DATE, LocalTime.of(8, 55));
        LocalDateTime checkOut = LocalDateTime.of(DATE, LocalTime.of(18, 5));
        Attendance att = buildAttendance(TodayStatus.CHECKED_OUT, checkIn, checkOut);
        when(attendanceDao.selectByEmpAndDate(EMP_ID, DATE)).thenReturn(att);

        AttendanceStatus result = service.recalculate(EMP_ID, DATE);

        assertEquals(AttendanceStatus.NORMAL, result);
    }

    @Test
    void testRecalculate_Late() {
        LocalDateTime checkIn = LocalDateTime.of(DATE, LocalTime.of(9, 15));
        LocalDateTime checkOut = LocalDateTime.of(DATE, LocalTime.of(18, 5));
        Attendance att = buildAttendance(TodayStatus.CHECKED_OUT, checkIn, checkOut);
        when(attendanceDao.selectByEmpAndDate(EMP_ID, DATE)).thenReturn(att);

        AttendanceStatus result = service.recalculate(EMP_ID, DATE);

        assertEquals(AttendanceStatus.LATE, result);
    }

    @Test
    void testRecalculate_Early() {
        LocalDateTime checkIn = LocalDateTime.of(DATE, LocalTime.of(8, 55));
        LocalDateTime checkOut = LocalDateTime.of(DATE, LocalTime.of(17, 30));
        Attendance att = buildAttendance(TodayStatus.CHECKED_OUT, checkIn, checkOut);
        when(attendanceDao.selectByEmpAndDate(EMP_ID, DATE)).thenReturn(att);

        AttendanceStatus result = service.recalculate(EMP_ID, DATE);

        assertEquals(AttendanceStatus.EARLY, result);
    }

    @Test
    void testRecalculate_LateAndEarly() {
        LocalDateTime checkIn = LocalDateTime.of(DATE, LocalTime.of(9, 15));
        LocalDateTime checkOut = LocalDateTime.of(DATE, LocalTime.of(17, 0));
        Attendance att = buildAttendance(TodayStatus.CHECKED_OUT, checkIn, checkOut);
        when(attendanceDao.selectByEmpAndDate(EMP_ID, DATE)).thenReturn(att);

        AttendanceStatus result = service.recalculate(EMP_ID, DATE);

        assertEquals(AttendanceStatus.LATE_EARLY, result);
    }

    @Test
    void testRecalculate_MissingCard() {
        LocalDateTime checkIn = LocalDateTime.of(DATE, LocalTime.of(8, 55));
        Attendance att = buildAttendance(TodayStatus.CHECKED_IN, checkIn, null);
        when(attendanceDao.selectByEmpAndDate(EMP_ID, DATE)).thenReturn(att);

        AttendanceStatus result = service.recalculate(EMP_ID, DATE);

        assertEquals(AttendanceStatus.MISSING_CARD, result);
    }

    @Test
    void testRecalculate_MissingCard_OnlyCheckOut() {
        LocalDateTime checkOut = LocalDateTime.of(DATE, LocalTime.of(18, 5));
        Attendance att = buildAttendance(TodayStatus.CHECKED_OUT, null, checkOut);
        when(attendanceDao.selectByEmpAndDate(EMP_ID, DATE)).thenReturn(att);

        AttendanceStatus result = service.recalculate(EMP_ID, DATE);

        assertEquals(AttendanceStatus.MISSING_CARD, result);
    }

    @Test
    void testRecalculate_Absence_NoSign() {
        Attendance att = buildAttendance(TodayStatus.NOT_CHECKED_IN, null, null);
        when(attendanceDao.selectByEmpAndDate(EMP_ID, DATE)).thenReturn(att);

        AttendanceStatus result = service.recalculate(EMP_ID, DATE);

        assertEquals(AttendanceStatus.ABSENCE, result);
    }

    @Test
    void testRecalculate_DateRange() {
        LocalDate start = LocalDate.of(2024, 3, 1);
        LocalDate end = LocalDate.of(2024, 3, 3);

        // 3月1日 - normal
        Attendance att1 = buildAttendance(TodayStatus.CHECKED_OUT,
                LocalDateTime.of(start, LocalTime.of(8, 55)),
                LocalDateTime.of(start, LocalTime.of(18, 5)));
        when(attendanceDao.selectByEmpAndDate(EMP_ID, start)).thenReturn(att1);

        // 3月2日 - holiday
        Attendance att2 = buildAttendance(TodayStatus.NOT_CHECKED_IN, null, null);
        when(attendanceDao.selectByEmpAndDate(EMP_ID, start.plusDays(1))).thenReturn(att2);
        when(holidayDao.selectHolidayTypeByDate(start.plusDays(1))).thenReturn("HOLIDAY");

        // 3月3日 - null (no record)
        when(attendanceDao.selectByEmpAndDate(EMP_ID, start.plusDays(2))).thenReturn(null);

        Map<LocalDate, AttendanceStatus> results = service.recalculate(EMP_ID, start, end);

        assertEquals(3, results.size());
        assertEquals(AttendanceStatus.NORMAL, results.get(start));
        assertEquals(AttendanceStatus.HOLIDAY, results.get(start.plusDays(1)));
        assertNull(results.get(start.plusDays(2)));
    }
}

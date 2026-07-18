package com.oa2.controller;

import com.oa2.pojo.Sign;
import com.oa2.service.SignService;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;

/**
 * 员工端 - 签到管理控制器
 */
@RestController
@RequestMapping("/attendance")
@CrossOrigin
public class SignController {

    @Autowired
    private SignService service;

    /**
     * 获取当前员工的签到列表
     */
    @GetMapping("/my-records")
    public RESP getMyAttendanceRecords(HttpSession session) {
        System.out.println("获取当前员工的签到列表");
        try {
            return service.empSignList(session);
        } catch (Exception e) {
            e.printStackTrace();
            return RESP.error("获取签到记录失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询当前员工的签到记录
     */
    @GetMapping("/my-records/page")
    public RESP getMyAttendanceRecordsByPage(
            @RequestParam(name = "currentPage", defaultValue = "1") Integer currentPage,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            HttpSession session) {

        try {
            return service.selectByPagehelper(currentPage, pageSize, session);
        } catch (Exception e) {
            e.printStackTrace();
            return RESP.error("获取签到记录失败：" + e.getMessage());
        }
    }

    /**
     * 员工签到
     */
    @PostMapping("/check-in")
    public RESP checkIn(@RequestBody Sign sign, HttpSession session, @RequestParam("coordinates") String coordinates) {
        try {
            return service.updateState(sign, session, coordinates);
        } catch (Exception e) {
            e.printStackTrace();
            return RESP.error("签到失败：" + e.getMessage());
        }
    }
}

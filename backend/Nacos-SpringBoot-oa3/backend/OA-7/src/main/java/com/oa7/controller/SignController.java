package com.oa7.controller;

import com.oa7.service.SignService;
import com.oa7.pojo.Sign;
import com.oa7.util.DU;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理员端 - 考勤管理控制器
 */
@RestController
@RequestMapping("/attendance")
@CrossOrigin
public class SignController {

    @Autowired
    private SignService signService;

    @GetMapping("/today/signed")
    public RESP todaySigned(@RequestParam int currentPage,
                            @RequestParam int pageSize) {
        return signService.todaySigned(currentPage, pageSize);
    }

    @GetMapping("/daily-statistics")
    public RESP dailyStatistics(@RequestParam int currentPage,
                                @RequestParam int pageSize) {
        return signService.dailyStatistics(currentPage, pageSize);
    }

    @GetMapping("/daily-details")
    public RESP dailyDetails(@RequestParam String date) {
        return signService.dailyDetails(date);
    }

    @GetMapping("/statistics/chart")
    public RESP chartData() {
        return signService.chartData();
    }

    @GetMapping("/unsigned")
    public RESP unsigned(@RequestParam int currentPage,
                         @RequestParam int pageSize) {
        return signService.unsigned(currentPage, pageSize);
    }

    @GetMapping("/today/unsigned")
    public RESP todayUnsigned(@RequestParam int currentPage,
                              @RequestParam int pageSize) {
        return signService.todayUnsigned(currentPage, pageSize);
    }

    @PutMapping("/{id}/approve")
    public String approve(@PathVariable int id) {
        return signService.approve(id);
    }
}

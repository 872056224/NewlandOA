package com.oa7.controller;

import com.oa7.service.SalaryService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/salary")
@CrossOrigin
public class SalaryController {

    @Autowired
    private SalaryService salaryService;

    @PostMapping("/calculate/{yearMonth}")
    public RESP calculate(@PathVariable String yearMonth) {
        return salaryService.calculate(yearMonth);
    }

    @GetMapping("/list/{yearMonth}")
    public RESP list(@PathVariable String yearMonth) {
        return salaryService.getByMonth(yearMonth);
    }
}

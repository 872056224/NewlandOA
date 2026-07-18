package com.oa7.controller;

import com.oa7.pojo.Duty;
import com.oa7.service.DutyService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/duties")
@CrossOrigin
public class DutyController {
    @Autowired private DutyService dutyService;

    @GetMapping
    public RESP list() {
        return dutyService.selectAll();
    }

    @PostMapping
    public String add(@RequestBody Duty duty) {
        return dutyService.add(duty);
    }

    @PutMapping("/{dutyId}")
    public String update(@PathVariable int dutyId, @RequestBody Duty duty) {
        return dutyService.update(dutyId, duty);
    }
}

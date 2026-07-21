package com.oa7.controller;

import com.oa7.service.RetroactiveSignService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/attendance/retroactive")
@CrossOrigin
public class RetroactiveSignController {

    @Autowired
    private RetroactiveSignService retroactiveSignService;

    @GetMapping("/pending")
    public RESP pending(@RequestParam(defaultValue = "1") int currentPage,
                        @RequestParam(defaultValue = "10") int pageSize) {
        return retroactiveSignService.getPending(currentPage, pageSize);
    }

    @PutMapping("/{id}/approve")
    public RESP approve(@PathVariable int id) {
        return retroactiveSignService.approve(id);
    }

    @PutMapping("/{id}/reject")
    public RESP reject(@PathVariable int id) {
        return retroactiveSignService.reject(id);
    }
}

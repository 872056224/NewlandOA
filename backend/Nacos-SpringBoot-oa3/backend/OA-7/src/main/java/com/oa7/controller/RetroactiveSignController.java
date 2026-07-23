package com.oa7.controller;

import com.oa7.service.RetroactiveSignService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/attendance/retroactive")
@CrossOrigin
public class RetroactiveSignController {

    @Autowired
    private RetroactiveSignService retroactiveSignService;

    @GetMapping("/pending")
    public RESP pending(@RequestParam(defaultValue = "1") int currentPage,
                        @RequestParam(defaultValue = "10") int pageSize,
                        HttpSession session) {
        return retroactiveSignService.getPending(currentPage, pageSize, session);
    }

    @PutMapping("/{id}/approve")
    public RESP approve(@PathVariable int id, HttpSession session) {
        return retroactiveSignService.approve(id, session);
    }

    @PutMapping("/{id}/reject")
    public RESP reject(@PathVariable int id, HttpSession session) {
        return retroactiveSignService.reject(id, session);
    }

    @PutMapping("/{id}/revoke")
    public RESP revoke(@PathVariable int id, HttpSession session) {
        return retroactiveSignService.revoke(id, session);
    }
}

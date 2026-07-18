package com.oa7.controller;

import com.oa7.pojo.KbDoc;
import com.oa7.service.KbDocService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员端 - AI 客服知识库管理
 *
 * 完整路由前缀：/api/v1/admin/kb-docs
 */
@RestController
@RequestMapping("/kb-docs")
@CrossOrigin
public class KbDocController {

    @Autowired
    private KbDocService kbDocService;

    @GetMapping
    public RESP list(@RequestParam int currentPage,
                     @RequestParam int pageSize,
                     @RequestParam(required = false) String keyword) {
        return kbDocService.selectPage(keyword, currentPage, pageSize);
    }

    @PostMapping
    public RESP add(@RequestParam int currentPage,
                    @RequestParam int pageSize,
                    @RequestParam(required = false) String keyword,
                    @RequestBody KbDoc kbDoc) {
        return kbDocService.add(kbDoc, keyword, currentPage, pageSize);
    }

    @PutMapping("/{id}")
    public RESP update(@PathVariable Integer id,
                       @RequestParam int currentPage,
                       @RequestParam int pageSize,
                       @RequestParam(required = false) String keyword,
                       @RequestBody KbDoc kbDoc) {
        return kbDocService.update(id, kbDoc, keyword, currentPage, pageSize);
    }

    @DeleteMapping("/{id}")
    public RESP delete(@PathVariable Integer id,
                       @RequestParam int currentPage,
                       @RequestParam int pageSize,
                       @RequestParam(required = false) String keyword) {
        return kbDocService.delete(id, keyword, currentPage, pageSize);
    }

    @PostMapping("/reload-index")
    public RESP reloadIndex() {
        return kbDocService.reloadIndex();
    }
}

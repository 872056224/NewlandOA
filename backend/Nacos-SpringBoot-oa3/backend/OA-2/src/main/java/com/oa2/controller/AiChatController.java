package com.oa2.controller;

import com.oa2.service.AiChatService;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 员工端 - AI 客服控制器
 *
 * 路由链路：前端 /api/v1/employee/ai/** -> 网关(8888) -> oa-emp-service(8081)
 * 受 LoginInterceptor 保护，仅登录员工可用
 */
@RestController
@RequestMapping("/ai")
@CrossOrigin
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    /**
     * AI 问答
     * body: {"question": "怎么签到打卡"}
     */
    @PostMapping("/chat")
    public RESP chat(@RequestBody Map<String, String> body) {
        try {
            return aiChatService.chat(body == null ? null : body.get("question"));
        } catch (Exception e) {
            e.printStackTrace();
            return RESP.error("AI 客服暂时不可用：" + e.getMessage());
        }
    }

    /**
     * 热门问题（对话页首屏推荐）
     */
    @GetMapping("/faq/hot")
    public RESP hotQuestions() {
        try {
            return aiChatService.hotQuestions();
        } catch (Exception e) {
            e.printStackTrace();
            return RESP.error("获取热门问题失败：" + e.getMessage());
        }
    }

    /**
     * 重建知识库向量索引（管理端 CRUD 后由 OA-7 内部调用，无需员工登录）
     */
    @PostMapping("/kb/reload")
    public RESP reloadIndex() {
        try {
            return aiChatService.reloadIndex();
        } catch (Exception e) {
            e.printStackTrace();
            return RESP.error("重建向量索引失败：" + e.getMessage());
        }
    }
}

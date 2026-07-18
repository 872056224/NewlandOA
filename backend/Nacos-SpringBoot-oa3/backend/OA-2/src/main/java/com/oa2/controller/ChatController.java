package com.oa2.controller;

import com.oa2.service.ChatService;
import com.oa2.util.RESP;
import com.oa2.vector.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 员工端 - AI 智能客服控制器
 *
 * 路由经网关与服务 context-path 后的完整前缀：/api/v1/employee/chat
 * 受 LoginInterceptor 保护，需登录后访问。
 */
@RestController
@RequestMapping("/chat")
@CrossOrigin
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    /** 用户单次提问的最大长度（防御超长输入） */
    private static final int MAX_QUESTION_LENGTH = 200;

    @Autowired
    private ChatService chatService;

    @Autowired
    private VectorStore vectorStore;

    /**
     * AI 问答（无状态接口，天然幂等）
     * Body: {"question": "怎么签到？"}
     */
    @PostMapping("/ask")
    public RESP ask(@RequestBody(required = false) Map<String, String> body) {
        String question = body == null ? null : body.get("question");
        if (question == null || question.trim().isEmpty()) {
            return RESP.error(400, "问题不能为空");
        }
        question = question.trim();
        if (question.length() > MAX_QUESTION_LENGTH) {
            return RESP.error(400, "问题过长，请控制在 " + MAX_QUESTION_LENGTH + " 字以内");
        }
        try {
            return RESP.ok(chatService.ask(question));
        } catch (Exception e) {
            log.error("AI 客服问答异常, question={}", question, e);
            return RESP.error("AI 客服暂时不可用，请稍后再试");
        }
    }

    /**
     * 推荐问题（开场引导）
     */
    @GetMapping("/suggestions")
    public RESP suggestions() {
        try {
            return RESP.ok(chatService.suggestions(6));
        } catch (Exception e) {
            log.error("获取推荐问题异常", e);
            return RESP.error("获取推荐问题失败");
        }
    }

    /**
     * 动态添加知识条目（写入自建向量库并持久化）
     * Body: {"question": "...", "answer": "...", "category": "可选"}
     */
    @PostMapping("/kb")
    public RESP addKnowledge(@RequestBody(required = false) Map<String, String> body) {
        String question = body == null ? null : body.get("question");
        String answer = body == null ? null : body.get("answer");
        if (question == null || question.trim().isEmpty()
                || answer == null || answer.trim().isEmpty()) {
            return RESP.error(400, "question 与 answer 不能为空");
        }
        if (question.length() > MAX_QUESTION_LENGTH || answer.length() > 2000) {
            return RESP.error(400, "内容过长：question 限 200 字，answer 限 2000 字");
        }
        try {
            String id = chatService.addKnowledge(question.trim(), answer.trim(),
                    body.get("category"));
            return RESP.ok(id);
        } catch (Exception e) {
            log.error("添加知识条目异常", e);
            return RESP.error("添加知识条目失败");
        }
    }

    /**
     * 向量库运行状态（条目数、维度、持久化路径）
     */
    @GetMapping("/kb/stats")
    public RESP stats() {
        return RESP.ok(vectorStore.stats());
    }
}

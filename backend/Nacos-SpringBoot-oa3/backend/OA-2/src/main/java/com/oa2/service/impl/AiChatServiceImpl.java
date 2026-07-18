package com.oa2.service.impl;

import com.oa2.dao.KbDocDao;
import com.oa2.pojo.KbDoc;
import com.oa2.service.AiChatService;
import com.oa2.service.LlmClient;
import com.oa2.util.RESP;
import com.oa2.vectorstore.LocalEmbedding;
import com.oa2.vectorstore.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 客服问答编排：
 * 用户问题 -> 本地向量化 -> 内存向量索引 TopK 余弦检索 -> 按置信度分层应答
 * （可选）配置 LLM 后，高/中置信命中会将知识注入 Prompt 走 RAG 生成式回答，
 * LLM 调用失败时自动降级为检索式回答，保证服务可用性。
 */
@Service
public class AiChatServiceImpl implements AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    /** 高置信阈值：直接采用知识库答案 */
    private static final float SCORE_HIGH = 0.55f;
    /** 低置信阈值：低于此分数视为未命中 */
    private static final float SCORE_LOW = 0.28f;
    private static final int TOP_K = 4;
    private static final int MAX_QUESTION_LENGTH = 500;

    private static final String FALLBACK_ANSWER =
            "抱歉，我暂时没有找到与您问题相关的答案。您可以换一种问法再试试，"
                    + "或联系部门管理员、行政部前台获取人工帮助（工作日 8:30 - 17:30）。";

    /** 知识库表不可用时的热门问题兜底（保证对话页首屏可用） */
    private static final List<String> DEFAULT_HOT_QUESTIONS = Arrays.asList(
            "怎么签到打卡", "怎么查看我的签到记录", "忘记密码怎么办", "怎么请假");

    @Autowired
    private KbDocDao kbDocDao;

    @Autowired
    private LocalEmbedding embedding;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private LlmClient llmClient;

    /**
     * 启动时从 MySQL 加载知识库并构建内存向量索引。
     * 表不存在或库连不上时仅告警不阻断启动，问答接口走兜底话术。
     */
    @PostConstruct
    public void buildIndex() {
        reloadIndexInternal();
    }

    @Override
    public RESP reloadIndex() {
        int size = reloadIndexInternal();
        if (size >= 0) {
            return RESP.ok(size);
        }
        return RESP.error("向量索引重建失败，请确认 kb_doc 表已创建");
    }

    /** @return 索引条目数；-1 表示失败 */
    private int reloadIndexInternal() {
        try {
            List<KbDoc> docs = kbDocDao.selectAllEnabled();
            List<VectorStore.Entry> entries = new ArrayList<>(docs.size());
            for (KbDoc doc : docs) {
                // 索引文本 = 标准问题 + 辅助关键词；答案不参与索引，避免干扰问句匹配
                String indexText = doc.getQuestion()
                        + " " + (doc.getKeywords() == null ? "" : doc.getKeywords());
                entries.add(new VectorStore.Entry(doc, embedding.embed(indexText)));
            }
            vectorStore.rebuild(entries);
            log.info("AI 客服向量索引构建完成，共 {} 条知识", entries.size());
            return entries.size();
        } catch (Exception e) {
            log.warn("AI 客服向量索引构建失败（请确认已执行 ai_kb.sql 建表），问答将走兜底话术: {}", e.getMessage());
            return -1;
        }
    }

    @Override
    public RESP chat(String question) {
        if (question == null || question.trim().isEmpty()) {
            return RESP.error(400, "问题不能为空");
        }
        String trimmed = question.trim();
        if (trimmed.length() > MAX_QUESTION_LENGTH) {
            return RESP.error(400, "问题过长，请控制在 " + MAX_QUESTION_LENGTH + " 字以内");
        }

        if (vectorStore.size() == 0) {
            reloadIndexInternal(); // 启动时建索引失败的场景：按需重试一次
        }
        if (vectorStore.size() == 0) {
            return RESP.ok(buildResult(FALLBACK_ANSWER, "fallback", null, 0f, DEFAULT_HOT_QUESTIONS));
        }

        List<VectorStore.Hit> hits = vectorStore.search(embedding.embed(trimmed), TOP_K);
        VectorStore.Hit best = hits.get(0);
        log.debug("AI 客服检索: q=[{}] best=[{}] score={}", trimmed, best.doc.getQuestion(), best.score);

        // 低置信：未命中，兜底 + 热门问题引导
        if (best.score < SCORE_LOW) {
            return RESP.ok(buildResult(FALLBACK_ANSWER, "fallback", null, best.score, hotList()));
        }

        List<String> related = relatedQuestions(hits);

        // 中置信：不直接给答案，返回"猜你想问"引导用户点击
        if (best.score < SCORE_HIGH) {
            String answer = "您是不是想问下面这些问题？点击即可查看答案：";
            return RESP.ok(buildResult(answer, "suggest", null, best.score, related));
        }

        // 高置信：优先 LLM 生成式回答（RAG），失败或未启用则用知识库答案直答
        if (llmClient.isAvailable()) {
            try {
                String llmAnswer = llmClient.complete(buildRagPrompt(hits), trimmed);
                return RESP.ok(buildResult(llmAnswer, "llm", best.doc.getQuestion(), best.score, related));
            } catch (Exception e) {
                log.warn("LLM 调用失败，降级为知识库直答: {}", e.getMessage());
            }
        }
        return RESP.ok(buildResult(best.doc.getAnswer(), "kb", best.doc.getQuestion(), best.score, related));
    }

    @Override
    public RESP hotQuestions() {
        try {
            List<String> hot = kbDocDao.selectHotQuestions();
            return RESP.ok(hot == null || hot.isEmpty() ? DEFAULT_HOT_QUESTIONS : hot);
        } catch (Exception e) {
            log.warn("查询热门问题失败: {}", e.getMessage());
            return RESP.ok(DEFAULT_HOT_QUESTIONS);
        }
    }

    private List<String> hotList() {
        try {
            List<String> hot = kbDocDao.selectHotQuestions();
            return hot == null || hot.isEmpty() ? DEFAULT_HOT_QUESTIONS : hot;
        } catch (Exception e) {
            return DEFAULT_HOT_QUESTIONS;
        }
    }

    /** 取 TopK 中达到低置信阈值的标准问题作为相关推荐（最多 3 条） */
    private List<String> relatedQuestions(List<VectorStore.Hit> hits) {
        List<String> related = new ArrayList<>();
        for (VectorStore.Hit hit : hits) {
            if (hit.score >= SCORE_LOW && related.size() < 3) {
                related.add(hit.doc.getQuestion());
            }
        }
        return related;
    }

    /** RAG 系统提示词：注入检索到的知识，约束模型只依据知识作答 */
    private String buildRagPrompt(List<VectorStore.Hit> hits) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 OA 办公系统的智能客服，负责解答员工关于系统使用的问题。\n");
        sb.append("请严格依据下面的知识条目回答，语气友好简洁；")
                .append("如果知识不足以回答，就建议用户联系管理员，不要编造功能。\n\n");
        int idx = 1;
        for (VectorStore.Hit hit : hits) {
            if (hit.score < SCORE_LOW) {
                continue;
            }
            sb.append("【知识").append(idx++).append("】\n");
            sb.append("问：").append(hit.doc.getQuestion()).append("\n");
            sb.append("答：").append(hit.doc.getAnswer()).append("\n\n");
        }
        return sb.toString();
    }

    private Map<String, Object> buildResult(String answer, String mode, String matchedQuestion,
                                            float score, List<String> suggestions) {
        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        result.put("mode", mode);
        result.put("matchedQuestion", matchedQuestion);
        result.put("score", Math.round(score * 1000f) / 1000f);
        result.put("suggestions", suggestions);
        return result;
    }
}

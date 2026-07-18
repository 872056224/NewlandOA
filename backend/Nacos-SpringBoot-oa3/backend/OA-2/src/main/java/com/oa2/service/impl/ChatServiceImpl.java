package com.oa2.service.impl;

import com.alibaba.fastjson.JSON;
import com.oa2.pojo.ChatAnswer;
import com.oa2.service.ChatService;
import com.oa2.vector.EmbeddingUtil;
import com.oa2.vector.SearchResult;
import com.oa2.vector.VectorDocument;
import com.oa2.vector.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI 客服问答实现：RAG 检索式问答（向量召回 + 阈值分级应答）
 *
 * 应答策略（按最佳相似度分级）：
 * - score >= 0.60：高置信，直接返回知识库答案，并附相关问题；
 * - 0.35 <= score < 0.60：中置信，以「猜你想问」口吻返回最接近的答案；
 * - score < 0.35：低置信，返回兜底话术并推荐可问的问题，避免答非所问。
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    private static final String KNOWLEDGE_BASE_PATH = "ai/knowledge-base.json";
    private static final String SOURCE_BUILTIN = "builtin";
    private static final String CATEGORY_CHITCHAT = "chitchat";

    /** 高置信阈值：直接应答 */
    private static final double DIRECT_ANSWER_THRESHOLD = 0.60;
    /** 中置信阈值：试探性应答；低于该值走兜底 */
    private static final double SUGGEST_THRESHOLD = 0.35;
    /** 检索召回条数（1 条主答案 + 最多 3 条相关问题） */
    private static final int RECALL_TOP_K = 4;

    @Autowired
    private VectorStore vectorStore;

    /**
     * 启动时加载内置知识库并构建向量索引
     */
    @PostConstruct
    public void initKnowledgeBase() {
        try (InputStream is = new ClassPathResource(KNOWLEDGE_BASE_PATH).getInputStream()) {
            String json = readAll(is);
            List<VectorDocument> docs = JSON.parseArray(json, VectorDocument.class);
            if (docs == null || docs.isEmpty()) {
                log.warn("内置知识库为空: {}", KNOWLEDGE_BASE_PATH);
                return;
            }
            int loaded = 0;
            for (VectorDocument doc : docs) {
                if (doc == null || doc.getId() == null || doc.getQuestion() == null || doc.getAnswer() == null) {
                    continue;
                }
                doc.setSource(SOURCE_BUILTIN);
                doc.setVector(null); // 强制按最新算法重建向量
                vectorStore.upsert(doc);
                loaded++;
            }
            log.info("AI 客服知识库加载完成：{} 条，向量维度 {}，当前向量库共 {} 条",
                    loaded, EmbeddingUtil.DIMENSION, vectorStore.size());
        } catch (Exception e) {
            // 知识库加载失败不阻断服务启动，问答接口将返回兜底回复
            log.error("AI 客服知识库加载失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public ChatAnswer ask(String question) {
        float[] queryVector = EmbeddingUtil.embed(question);
        List<SearchResult> hits = vectorStore.search(queryVector, RECALL_TOP_K, 0.05);

        ChatAnswer answer = new ChatAnswer();
        if (hits.isEmpty()) {
            return fallback(answer);
        }

        SearchResult best = hits.get(0);
        double score = best.getScore();
        answer.setConfidence(round2(score));

        if (score >= DIRECT_ANSWER_THRESHOLD) {
            answer.setMatched(true);
            answer.setAnswer(best.getDocument().getAnswer());
            answer.setRelated(buildRelated(hits));
        } else if (score >= SUGGEST_THRESHOLD) {
            answer.setMatched(true);
            answer.setAnswer("您是想问「" + best.getDocument().getQuestion() + "」吧？\n\n"
                    + best.getDocument().getAnswer());
            answer.setRelated(buildRelated(hits));
        } else {
            fallback(answer);
        }
        return answer;
    }

    @Override
    public List<String> suggestions(int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }
        List<String> questions = vectorStore.listAll().stream()
                .filter(d -> !CATEGORY_CHITCHAT.equals(d.getCategory()))
                .map(VectorDocument::getQuestion)
                .collect(Collectors.toList());
        Collections.shuffle(questions);
        return questions.size() <= count ? questions : questions.subList(0, count);
    }

    @Override
    public String addKnowledge(String question, String answer, String category) {
        VectorDocument doc = new VectorDocument();
        doc.setId("custom-" + UUID.randomUUID());
        doc.setQuestion(question);
        doc.setAnswer(answer);
        doc.setCategory(category == null || category.trim().isEmpty() ? "custom" : category.trim());
        doc.setSource("custom");
        vectorStore.upsert(doc);
        log.info("AI 客服新增知识条目: id={}, question={}", doc.getId(), question);
        return doc.getId();
    }

    /** 除最佳命中外的其余召回，作为「相关问题」返回（寒暄类不推荐） */
    private List<ChatAnswer.Related> buildRelated(List<SearchResult> hits) {
        List<ChatAnswer.Related> related = new ArrayList<>();
        for (int i = 1; i < hits.size(); i++) {
            SearchResult hit = hits.get(i);
            if (hit.getScore() < SUGGEST_THRESHOLD * 0.6) {
                continue;
            }
            if (CATEGORY_CHITCHAT.equals(hit.getDocument().getCategory())) {
                continue;
            }
            related.add(new ChatAnswer.Related(hit.getDocument().getQuestion(), round2(hit.getScore())));
        }
        return related;
    }

    private ChatAnswer fallback(ChatAnswer answer) {
        answer.setMatched(false);
        answer.setAnswer("抱歉，这个问题我还没学会 (>_<)\n您可以换个问法，或者试试下面这些问题：");
        answer.setSuggestions(suggestions(3));
        return answer;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** Java 8 兼容的流读取 */
    private static String readAll(InputStream is) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int len;
        while ((len = is.read(chunk)) != -1) {
            buffer.write(chunk, 0, len);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }
}

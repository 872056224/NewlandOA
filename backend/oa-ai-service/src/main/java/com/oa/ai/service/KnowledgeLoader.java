package com.oa.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 知识库 ETL：启动时将公司知识文档切块、向量化并灌入 Redis Stack 向量库
 *
 * 幂等策略：每次启动先清理键前缀下的内置知识块（kb-chunk-*）再重灌，
 * 保证知识文档修改后重启即生效，且不产生残留旧块；
 * custom-* 前缀的运行期动态知识不受清理影响，持久保留在 Redis 中。
 */
@Component
public class KnowledgeLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeLoader.class);

    /** 内置知识块的文档 id 前缀（区别于运行期动态添加的 custom-*） */
    private static final String BUILTIN_DOC_ID_PREFIX = "kb-chunk-";

    private final VectorStore vectorStore;

    @Value("${ai.assistant.knowledge-path:knowledge/company.md}")
    private String knowledgePath;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6380}")
    private int redisPort;

    @Value("${spring.ai.vectorstore.redis.prefix:oa:kb:}")
    private String redisKeyPrefix;

    /** 向量库模式：redis（默认，持久化）/ memory（内存降级，无需 Redis Stack） */
    @Value("${ai.assistant.vector-mode:redis}")
    private String vectorMode;

    public KnowledgeLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String markdown = loadKnowledgeFile();
            List<Document> chunks = splitByMarkdownSection(markdown);
            if (chunks.isEmpty()) {
                log.warn("知识文档无有效章节，跳过灌库: {}", knowledgePath);
                return;
            }
            // 内存模式没有历史残留，无需（也无法）做 Redis 键清理
            int removed = "memory".equalsIgnoreCase(vectorMode) ? 0 : cleanBuiltinChunks();
            vectorStore.add(chunks);
            log.info("知识库灌库完成（{} 模式）：清理旧块 {} 个，写入 {} 个章节块", vectorMode, removed, chunks.size());
        } catch (Exception e) {
            // 灌库失败不阻断服务启动：RAG 检索为空时模型仍按系统提示词应答，前端另有 FAQ 降级
            log.error("知识库灌库失败（服务继续运行，RAG 将无知识可检索）: {}", e.getMessage(), e);
        }
    }

    private String loadKnowledgeFile() throws Exception {
        ClassPathResource resource = new ClassPathResource(knowledgePath);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * 按 Markdown 二级标题（## 章节）切块：每块语义完整、粒度适中（200~500 字），
     * 标题随块保留以增强 embedding 的主题区分度
     */
    private List<Document> splitByMarkdownSection(String markdown) {
        List<Document> documents = new ArrayList<>();
        String[] sections = markdown.split("(?m)^## ");
        int index = 0;
        for (String section : sections) {
            String content = section.trim();
            // 跳过文档主标题等无正文内容的片段
            if (content.isEmpty() || content.startsWith("#")) {
                continue;
            }
            String title = content.lines().findFirst().orElse("未命名章节").trim();
            documents.add(Document.builder()
                    .id(BUILTIN_DOC_ID_PREFIX + index)
                    .text("## " + content)
                    .metadata(Map.of(
                            "section", title,
                            "source", "company.md",
                            "type", "builtin"))
                    .build());
            index++;
        }
        return documents;
    }

    /**
     * 清理 Redis 中的内置知识块（SCAN 渐进式遍历，避免 KEYS 阻塞）
     */
    private int cleanBuiltinChunks() {
        int removed = 0;
        try (JedisPooled jedis = new JedisPooled(redisHost, redisPort)) {
            ScanParams params = new ScanParams().match(redisKeyPrefix + BUILTIN_DOC_ID_PREFIX + "*").count(200);
            String cursor = "0";
            do {
                ScanResult<String> result = jedis.scan(cursor, params);
                cursor = result.getCursor();
                for (String key : result.getResult()) {
                    jedis.del(key);
                    removed++;
                }
            } while (!"0".equals(cursor));
        } catch (Exception e) {
            log.warn("清理旧知识块失败（可能产生重复/残留块，不影响检索可用性）: {}", e.getMessage());
        }
        return removed;
    }
}

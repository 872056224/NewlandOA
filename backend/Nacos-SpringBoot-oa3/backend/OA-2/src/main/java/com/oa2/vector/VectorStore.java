package com.oa2.vector;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 自建轻量级向量数据库
 *
 * 设计要点：
 * - 存储：ConcurrentHashMap 全量驻留内存，FAQ 级数据量（万条以内）暴力余弦扫描性能足够；
 * - 检索：TopK 最小堆 + 相似度阈值过滤，时间复杂度 O(N * D + N * logK)；
 * - 持久化：仅 custom 来源的文档落盘为 JSON（builtin 由知识库每次启动重建，保证 FAQ 更新即时生效），
 *   落盘失败仅告警不影响内存服务（优雅降级）；
 * - 线程安全：并发容器承载读写，落盘操作 synchronized 串行化。
 */
// 显式命名：与 com.oa2.vectorstore.VectorStore 类名相同，默认 bean 名都是 vectorStore 会冲突
@Component("chatVectorStore")
public class VectorStore {

    private static final Logger log = LoggerFactory.getLogger(VectorStore.class);

    private static final String SOURCE_CUSTOM = "custom";

    private final Map<String, VectorDocument> documents = new ConcurrentHashMap<>();

    /** 持久化文件路径（相对服务运行目录） */
    @Value("${ai.vector-store.persist-path:./data/vector-store.json}")
    private String persistPath;

    /**
     * 启动时恢复运行期动态添加的知识条目
     */
    @PostConstruct
    public void loadFromDisk() {
        try {
            Path path = Paths.get(persistPath);
            if (!Files.exists(path)) {
                log.info("向量库持久化文件不存在，跳过恢复: {}", persistPath);
                return;
            }
            byte[] bytes = Files.readAllBytes(path);
            String json = new String(bytes, StandardCharsets.UTF_8);
            List<VectorDocument> restored = JSON.parseArray(json, VectorDocument.class);
            if (restored == null) {
                return;
            }
            int count = 0;
            for (VectorDocument doc : restored) {
                if (doc == null || doc.getId() == null || isBlank(doc.getQuestion())) {
                    continue;
                }
                // 维度不符（如算法升级后）则基于问题文本重建向量
                if (doc.getVector() == null || doc.getVector().length != EmbeddingUtil.DIMENSION) {
                    doc.setVector(EmbeddingUtil.embed(doc.getQuestion()));
                }
                doc.setSource(SOURCE_CUSTOM);
                documents.put(doc.getId(), doc);
                count++;
            }
            log.info("向量库已从磁盘恢复 {} 条 custom 知识条目", count);
        } catch (Exception e) {
            log.warn("向量库持久化文件读取失败，以纯内存模式启动: {}", e.getMessage());
        }
    }

    /**
     * 新增或更新一条文档；未携带合法向量时自动基于 question 编码
     */
    public void upsert(VectorDocument doc) {
        if (doc == null || isBlank(doc.getId()) || isBlank(doc.getQuestion())) {
            throw new IllegalArgumentException("向量文档的 id 与 question 不能为空");
        }
        if (doc.getVector() == null || doc.getVector().length != EmbeddingUtil.DIMENSION) {
            doc.setVector(EmbeddingUtil.embed(doc.getQuestion()));
        }
        documents.put(doc.getId(), doc);
        if (SOURCE_CUSTOM.equals(doc.getSource())) {
            persist();
        }
    }

    /**
     * 删除文档
     *
     * @return 是否真实删除
     */
    public boolean delete(String id) {
        if (isBlank(id)) {
            return false;
        }
        VectorDocument removed = documents.remove(id);
        if (removed != null && SOURCE_CUSTOM.equals(removed.getSource())) {
            persist();
        }
        return removed != null;
    }

    public VectorDocument get(String id) {
        return id == null ? null : documents.get(id);
    }

    public List<VectorDocument> listAll() {
        return new ArrayList<>(documents.values());
    }

    public int size() {
        return documents.size();
    }

    /**
     * TopK 余弦相似度检索
     *
     * @param queryVector 查询向量（应已 L2 归一化）
     * @param topK        最多返回条数
     * @param minScore    相似度下限，低于该值的文档直接过滤
     * @return 按相似度降序排列的命中列表
     */
    public List<SearchResult> search(float[] queryVector, int topK, double minScore) {
        if (queryVector == null || queryVector.length != EmbeddingUtil.DIMENSION || topK <= 0) {
            return Collections.emptyList();
        }
        PriorityQueue<SearchResult> heap = new PriorityQueue<>(Comparator.comparingDouble(SearchResult::getScore));
        for (VectorDocument doc : documents.values()) {
            double score = EmbeddingUtil.cosine(queryVector, doc.getVector());
            if (score < minScore) {
                continue;
            }
            heap.offer(new SearchResult(doc, score));
            if (heap.size() > topK) {
                heap.poll();
            }
        }
        List<SearchResult> results = new ArrayList<>(heap);
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return results;
    }

    /**
     * 向量库运行状态（供监控/排查）
     */
    public Map<String, Object> stats() {
        long builtinCount = documents.values().stream()
                .filter(d -> !SOURCE_CUSTOM.equals(d.getSource()))
                .count();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalDocuments", documents.size());
        stats.put("builtinDocuments", builtinCount);
        stats.put("customDocuments", documents.size() - builtinCount);
        stats.put("dimension", EmbeddingUtil.DIMENSION);
        stats.put("persistPath", persistPath);
        return stats;
    }

    /**
     * 仅持久化 custom 文档；失败不抛出，保证主流程可用
     */
    private synchronized void persist() {
        try {
            List<VectorDocument> customDocs = documents.values().stream()
                    .filter(d -> SOURCE_CUSTOM.equals(d.getSource()))
                    .collect(Collectors.toList());
            Path path = Paths.get(persistPath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            String json = JSON.toJSONString(customDocs, SerializerFeature.PrettyFormat);
            Files.write(path, json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("向量库落盘失败（内存数据不受影响）: {}", e.getMessage());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

package com.oa2.vectorstore;

import com.oa2.pojo.KbDoc;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 内存向量索引（自建向量数据库的检索层）
 *
 * 文档持久化在 MySQL kb_doc 表，本类只负责向量的存储与 TopK 余弦检索。
 * FAQ 量级（百~千条）下暴力扫描 O(n*dim) 的延迟在亚毫秒级，
 * 无需引入 HNSW 等近似索引，简单即正确。
 *
 * 并发模型：写少读多。重建索引时整体替换 volatile 快照引用，
 * 查询无锁读取当前快照，天然线程安全。
 */
@Component
public class VectorStore {

    /** 索引条目：文档 + 其向量 */
    public static class Entry {
        public final KbDoc doc;
        public final float[] vector;

        public Entry(KbDoc doc, float[] vector) {
            this.doc = doc;
            this.vector = vector;
        }
    }

    /** 检索命中结果 */
    public static class Hit {
        public final KbDoc doc;
        public final float score;

        public Hit(KbDoc doc, float score) {
            this.doc = doc;
            this.score = score;
        }
    }

    private volatile List<Entry> snapshot = Collections.emptyList();

    /** 全量重建索引（启动加载时调用） */
    public void rebuild(List<Entry> entries) {
        this.snapshot = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /**
     * TopK 余弦相似度检索
     *
     * @param queryVector 已归一化的查询向量
     * @param topK        返回条数上限
     * @return 按相似度降序排列的命中列表
     */
    public List<Hit> search(float[] queryVector, int topK) {
        List<Entry> entries = this.snapshot;
        List<Hit> hits = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            float score = LocalEmbedding.cosine(queryVector, entry.vector);
            hits.add(new Hit(entry.doc, score));
        }
        hits.sort(Comparator.comparingDouble((Hit h) -> h.score).reversed());
        if (hits.size() > topK) {
            return new ArrayList<>(hits.subList(0, topK));
        }
        return hits;
    }

    public int size() {
        return snapshot.size();
    }
}

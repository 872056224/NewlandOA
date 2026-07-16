package com.oa2.vectorstore;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地文本向量化引擎（零外部依赖，离线可用）
 *
 * 算法：Signed Feature Hashing
 * 1. 分词：中文按 单字(unigram) + 相邻双字(bigram) 提取特征，
 *    连续的英文/数字串作为整词特征（如 "GPS"、"30"）
 * 2. 每个特征经哈希映射到固定维度的桶位，第二个哈希决定符号(+1/-1)，
 *    带符号累加可抵消哈希碰撞带来的系统性偏差
 * 3. bigram 权重高于 unigram（双字组合的语义区分度更强）
 * 4. L2 归一化 —— 归一化后两向量的点积即余弦相似度
 *
 * 该实现是确定性函数，向量无需持久化，重启后重建索引结果一致。
 */
@Component
public class LocalEmbedding {

    /** 向量维度：FAQ 量级下 512 维的碰撞率已足够低 */
    public static final int DIM = 512;

    private static final float WEIGHT_UNIGRAM = 1.0f;
    private static final float WEIGHT_BIGRAM = 2.0f;
    private static final float WEIGHT_ASCII_WORD = 1.5f;

    /**
     * 文本向量化
     *
     * @param text 原始文本（容忍 null / 空串，返回零向量）
     * @return 长度为 DIM 的 L2 归一化向量
     */
    public float[] embed(String text) {
        float[] vector = new float[DIM];
        if (text == null || text.trim().isEmpty()) {
            return vector;
        }

        List<String> chineseChars = new ArrayList<>();
        StringBuilder asciiWord = new StringBuilder();

        String normalized = text.toLowerCase();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                asciiWord.append(c);
                continue;
            }
            // 遇到非字母数字，结算当前英文/数字词
            if (asciiWord.length() > 0) {
                addFeature(vector, "w:" + asciiWord, WEIGHT_ASCII_WORD);
                asciiWord.setLength(0);
            }
            if (isCjk(c)) {
                chineseChars.add(String.valueOf(c));
            }
            // 标点、空白等其余字符直接忽略
        }
        if (asciiWord.length() > 0) {
            addFeature(vector, "w:" + asciiWord, WEIGHT_ASCII_WORD);
        }

        for (int i = 0; i < chineseChars.size(); i++) {
            addFeature(vector, "u:" + chineseChars.get(i), WEIGHT_UNIGRAM);
            if (i + 1 < chineseChars.size()) {
                addFeature(vector, "b:" + chineseChars.get(i) + chineseChars.get(i + 1), WEIGHT_BIGRAM);
            }
        }

        normalize(vector);
        return vector;
    }

    /** 余弦相似度（向量已归一化时等价于点积） */
    public static float cosine(float[] a, float[] b) {
        float dot = 0f;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }

    private void addFeature(float[] vector, String feature, float weight) {
        int h1 = fnv1a(feature, 0x811C9DC5);
        int h2 = fnv1a(feature, 0x01000193);
        int bucket = Math.floorMod(h1, DIM);
        float sign = (h2 & 1) == 0 ? 1f : -1f;
        vector[bucket] += sign * weight;
    }

    /** FNV-1a 变种，seed 不同得到相互独立的哈希值 */
    private int fnv1a(String s, int seed) {
        int hash = seed;
        for (int i = 0; i < s.length(); i++) {
            hash ^= s.charAt(i);
            hash *= 0x01000193;
        }
        return hash;
    }

    private boolean isCjk(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF)      // CJK 统一表意文字
                || (c >= 0x3400 && c <= 0x4DBF); // CJK 扩展 A
    }

    private void normalize(float[] vector) {
        float sumSq = 0f;
        for (float v : vector) {
            sumSq += v * v;
        }
        if (sumSq <= 0f) {
            return;
        }
        float norm = (float) Math.sqrt(sumSq);
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }
    }
}

package com.oa2.vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 轻量级文本向量化工具（纯 Java 实现，无外部模型/服务依赖）
 *
 * 实现原理：
 * 1. 分词：中文按 unigram + bigram 切分（bigram 捕捉词序信息），英文/数字按连续串整词切分；
 * 2. 特征哈希（hashing trick）：token 经 FNV-1a 哈希映射到固定维度槽位，
 *    用第二个独立哈希决定符号(+1/-1)，抵消哈希碰撞带来的系统性偏差；
 * 3. L2 归一化：归一化后两向量的余弦相似度可直接用点积计算。
 *
 * 该方案对 FAQ 级短文本检索效果良好，且零依赖、毫秒级延迟。
 */
public final class EmbeddingUtil {

    /** 向量维度（FAQ 级语料规模下 256 维足够区分） */
    public static final int DIMENSION = 256;

    /** 中文语气词等停用字，对语义贡献低，分词时剔除 */
    private static final Set<Character> STOP_CHARS = new HashSet<>(Arrays.asList(
            '的', '了', '吗', '呢', '啊', '哦', '呀', '嘛', '吧', '哟', '咯', '呗', '噢', '嗯', '哎'
    ));

    private EmbeddingUtil() {
    }

    /**
     * 将文本编码为 L2 归一化的固定维度向量
     *
     * @param text 任意文本（null/空白返回零向量）
     * @return 长度为 {@link #DIMENSION} 的归一化向量
     */
    public static float[] embed(String text) {
        float[] vector = new float[DIMENSION];
        List<String> tokens = tokenize(text);
        if (tokens.isEmpty()) {
            return vector;
        }
        for (String token : tokens) {
            int h1 = fnv1a(token, 0x811C9DC5);
            int h2 = fnv1a(token, 0x01000193);
            int index = Math.floorMod(h1, DIMENSION);
            int sign = (h2 & 1) == 0 ? 1 : -1;
            vector[index] += sign;
        }
        normalize(vector);
        return vector;
    }

    /**
     * 余弦相似度。两入参均已 L2 归一化时等价于点积。
     */
    public static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }

    /**
     * 混合分词：中文产出 unigram + bigram，英文/数字小写整词
     */
    static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) {
            return tokens;
        }
        StringBuilder cjk = new StringBuilder();
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isCjk(c)) {
                flushWord(word, tokens);
                if (!STOP_CHARS.contains(c)) {
                    cjk.append(c);
                }
            } else if (Character.isLetterOrDigit(c)) {
                flushCjk(cjk, tokens);
                word.append(Character.toLowerCase(c));
            } else {
                // 标点、空白等一律视为分隔符
                flushCjk(cjk, tokens);
                flushWord(word, tokens);
            }
        }
        flushCjk(cjk, tokens);
        flushWord(word, tokens);
        return tokens;
    }

    private static void flushCjk(StringBuilder cjk, List<String> tokens) {
        int len = cjk.length();
        if (len == 0) {
            return;
        }
        for (int i = 0; i < len; i++) {
            tokens.add(String.valueOf(cjk.charAt(i)));
            if (i + 1 < len) {
                tokens.add(cjk.substring(i, i + 2));
            }
        }
        cjk.setLength(0);
    }

    private static void flushWord(StringBuilder word, List<String> tokens) {
        if (word.length() == 0) {
            return;
        }
        tokens.add(word.toString());
        word.setLength(0);
    }

    private static boolean isCjk(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }

    /** 带种子的 FNV-1a 哈希，不同种子可视作两个独立哈希函数 */
    private static int fnv1a(String s, int seed) {
        int hash = seed;
        for (int i = 0; i < s.length(); i++) {
            hash ^= s.charAt(i);
            hash *= 0x01000193;
        }
        return hash;
    }

    private static void normalize(float[] vector) {
        double sumSq = 0.0;
        for (float v : vector) {
            sumSq += v * v;
        }
        double norm = Math.sqrt(sumSq);
        if (norm < 1e-9) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= (float) norm;
        }
    }
}

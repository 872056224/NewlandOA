package com.oa7.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import java.time.Duration;

@Component
public class JediPoolUtil {
    // Redis服务器地址(域名或IP)
    @Value("${redis.host}")
    private String host;
    // Redis服务器连接端⼝(Redis默认端⼝号是6379)
    @Value("${redis.port}")
    private String port;
    // Redis服务器连接密码(默认为空)
    @Value("${redis.password}")
    private String password;
    // 最⼤连接数
    @Value("${redis.maxTotal}")
    private String maxTotal;
    // 最⼤空闲连接数
    @Value("${redis.maxIdle}")
    private String maxIdle;
    // 最⼤的阻塞时⻓
    @Value("${redis.maxWait}")
    private String maxWait;
    // 向资源池借⽤连接时是否做连接有效性检测（ping）。检测到的⽆效连接将会被移除
    @Value("${redis.testOnBorrow}")
    private String testOnBorrow;
    private volatile static JedisPool jedisPool = null;
    private volatile static Jedis jedis = null;

    private JediPoolUtil() {
    }

    private  JedisPool getInstance() {
        // 单例模式实现：双检锁/双重校验锁。这种⽅式采⽤双锁机制，安全且在多线程情况下能保持⾼性能
        if (jedisPool == null) {
            synchronized (JediPoolUtil.class) {
                if (jedisPool == null) {
                    // 创建⼀个配置对象
                    JedisPoolConfig config = new JedisPoolConfig();
                    config.setMaxTotal(Integer.parseInt(maxTotal)); // 资源池中的最⼤连接数
                    config.setMaxIdle(Integer.parseInt(maxIdle)); // 资源池允许的最⼤空闲连接数
                    // 当资源池连接⽤尽后，调⽤者的最⼤等待时间(单位为毫秒)
                    config.setMaxWait(Duration.ofMillis(Integer.parseInt(maxWait)));
                    // 向资源池借⽤连接时是否做连接有效性检测(业务量很⼤时候建议设置为false，减少⼀次ping的开销)
                    config.setTestOnBorrow(Boolean.valueOf(testOnBorrow));
                    // 初始化JedisPool
                    jedisPool = new JedisPool(config, host, Integer.parseInt(port));
                }
            }
        }
        return jedisPool;
    }

    /**
     * 获取连接⽅法
     */
    public  Jedis getJedis() {
        if (jedis == null) {
            // 获取连接
            jedis = getInstance().getResource();
        }
        return jedis;
    }
}

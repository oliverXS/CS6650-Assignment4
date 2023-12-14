package config;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import util.Constants;

/**
 * @author xiaorui
 */
public class RedisConfig {
    private static final JedisPool pool;
    static {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(50);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(5);
        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setMaxWaitMillis(1000);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRunsMillis(30000);
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setMinEvictableIdleTimeMillis(60000);
        poolConfig.setJmxEnabled(false);

        pool = new JedisPool(poolConfig, Constants.REDIS_HOST, Constants.REDIS_PORT);
    }

    public static JedisPool getPool() {
        return pool;
    }
}


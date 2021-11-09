package cn.shinema.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class RedisCommonConfig {
    private static final Logger logger = LoggerFactory.getLogger(RedisCommonConfig.class);

    @Value("${redis.jedis.max-active:8}")
    public int maxActive;

    @Value("${redis.jedis.max-idle:8}")
    public int maxIdle;

    @Value("${redis.jedis.min-idle:4}")
    public int minIdle;

    @Value("${redis.jedis.minEvictableidleTimeMillis:10000}")
    public long minEvictableidleTimeMillis;

    @Value("${redis.jedis.timeBetweenEvictionRunsMillis:20000}")
    public long timeBetweenEvictionRunsMillis;

    @Value("${redis.jedis.readTimeout:2000}")
    public int readTimeout;

    @Value("${redis.jedis.connectTimeout:2000}")
    public int connectTimeout;

    public JedisConnectionFactory jedisConnectionFactory(String hostname, int port, String password, int database) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(hostname);
        config.setPort(port);
        config.setPassword(RedisPassword.of(password));
        config.setDatabase(database);
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(config, createJedisConfiguration());
        logger.info("JEDIS客户端初始化完成,host:{},port:{},database:{}", hostname, port, database);
        return jedisConnectionFactory;
    }

    public JedisClientConfiguration createJedisConfiguration() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxActive);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setTestOnBorrow(false);
        config.setTestWhileIdle(true);
        config.setMaxWaitMillis(-1);
        config.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        config.setMinEvictableIdleTimeMillis(minEvictableidleTimeMillis);
        JedisClientConfiguration.JedisPoolingClientConfigurationBuilder builder = JedisClientConfiguration.builder().usePooling();
        builder.poolConfig(config);
        builder.and().readTimeout(Duration.ofMillis(readTimeout)).connectTimeout(Duration.ofMillis(connectTimeout));
        return builder.build();
    }

    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, int ttl, String cacheName) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())) // 序列化
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new JdkSerializationRedisSerializer()))//
                .entryTtl(Duration.ofSeconds(ttl)) // 失效时间
                // .prefixKeysWith("agent:") //前缀
                .disableCachingNullValues();

        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(redisConnectionFactory);
        builder.cacheDefaults(config);
        builder.transactionAware();
        if (cacheName != null) {
            Set<String> cacheNames = new HashSet<String>();
            cacheNames.add(cacheName);
            builder.initialCacheNames(cacheNames);
        }
        return builder.build();
    }

    public RedisTemplate<String, String> getRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        RedisSerializer<String> stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
        template.setEnableDefaultSerializer(true);
        template.setConnectionFactory(connectionFactory);
        return template;
    }
}

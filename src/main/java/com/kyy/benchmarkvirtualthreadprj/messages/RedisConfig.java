package com.kyy.benchmarkvirtualthreadprj.messages;

import jakarta.annotation.PostConstruct;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableCaching
@RequiredArgsConstructor
public class RedisConfig {

    private final RedisConnectionFactory redisConnectionFactory;  // ★ Boot에서 자동 생성됨
    private final RedisProperties redisProperties;                // ★ yml 값 자동 바인딩됨

    @PostConstruct
    public void testRedisConnection() {
        try {
            var conn = redisConnectionFactory.getConnection();
            System.out.println("🔥 Redis PING: " + conn.ping());
            System.out.println("🔥 Redis HOST = " + redisProperties.getHost());
            System.out.println("🔥 Redis PORT = " + redisProperties.getPort());
        } catch (Exception e) {
            System.out.println("❌ Redis 연결 실패");
            e.printStackTrace();
        }
    }

    // RedisTemplate
    @Bean
    public StringRedisTemplate stringRedisTemplate() {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    // Pub/Sub Listener
    @Bean
    public MessageListenerAdapter alarmListenerAdapter(AlarmPubSubSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            MessageListenerAdapter listenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory); // ★ 자동설정 사용
        container.addMessageListener(listenerAdapter, new PatternTopic("alarm:update:*"));
        return container;
    }

    // CacheManager
    @Bean
    public RedisCacheManager cacheManager() {
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofHours(1))
                )
                .build();
    }
}

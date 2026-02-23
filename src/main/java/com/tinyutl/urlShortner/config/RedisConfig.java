package com.tinyutl.urlShortner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Use plain String serializer for keys so they look legible in redis-cli
        template.setKeySerializer(new StringRedisSerializer());
        // Use generic string serializer for values so numbers are stored correctly for
        // increment operations
        template.setValueSerializer(new GenericToStringSerializer<>(Object.class));

        return template;
    }
}

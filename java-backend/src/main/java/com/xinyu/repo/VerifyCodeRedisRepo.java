package com.xinyu.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
public class VerifyCodeRedisRepo {
    private final StringRedisTemplate redis;
    private final ObjectMapper om = new ObjectMapper();

    public VerifyCodeRedisRepo(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String tokenKey(String vid) { return "vc:token:" + vid; }
    public String cdKey(String receiver) { return "vc:cd:" + receiver; }
    public String tryKey(String vid) { return "vc:try:" + vid; }
    public String lockKey(String receiver) { return "vc:lock:" + receiver; }

    public Optional<Long> ttlSeconds(String key) {
        Long t = redis.getExpire(key);
        return Optional.ofNullable(t);
    }

    public boolean exists(String key) {
        Boolean v = redis.hasKey(key);
        return v != null && v;
    }

    public void set(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(redis.opsForValue().get(key));
    }

    public long incr(String key, Duration ttlIfNew) {
        Long v = redis.opsForValue().increment(key);
        if (v != null && v == 1L) {
            redis.expire(key, ttlIfNew);
        }
        return v == null ? 0 : v;
    }

    public void del(String key) {
        redis.delete(key);
    }

    public String toJson(Object obj) {
        try { return om.writeValueAsString(obj); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public <T> T fromJson(String json, Class<T> cls) {
        try { return om.readValue(json, cls); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}

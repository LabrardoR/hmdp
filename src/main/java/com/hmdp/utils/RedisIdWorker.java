package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 开始时间戳
     * 2024-01-01-00:00
     */
    private static final long BEGIN_TIMESTAMP = 1704067200L;
    /**
     * 序列号的位数
     */
    private static final int COUNT_BITS = 32;

    /**
     * 实现全局唯一 ID
     * @param keyPrefix
     * @return
     */
    public long nextId(String keyPrefix){
        // 1. 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        // 2. 生成序列号
        // 获取当前日期，精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 自增长
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        // 3. 拼接并返回
        return timestamp << COUNT_BITS | count;
    }


}

package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

// todo 未完成 视频p9
@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }


    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 将任意对象转为json并存储在string类型的key中，设置数据为逻辑过期
     * @param key
     * @param value
     * @param time
     * @param unit
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        // 设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入 Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }


    /**
     * 根据指定的key查询缓存，并反序列化为指定类型，利用缓存空值的方式解决缓存穿透问题
     * @param keyPrefix Redis中数据前缀
     * @param id 查询id
     * @param type 返回类型
     * @param dbFallback 从数据库查询的方法
     * @param time 缓存时间
     * @param unit 缓存时间单位
     * @return 查询到的数据
     * @param <R> 返回类型
     * @param <ID> 查询id类型
     */
    public  <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 从 redis 查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 判断是否存在
        if(StrUtil.isNotBlank(json)){
            // 存在，直接返回
            R value = JSONUtil.toBean(json, type);
            return value;
        }
        // 命中的为空值，说明是将空值写入了 Redis
        if(json != null){
            return null;
        }

        // 不存在，根据id查询数据库
        R r = dbFallback.apply(id);
        // 不存在，返回
        if(r == null){
            // 将空值写入 redis
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);// 设置了一个比较短的TTL
            return null;
        }
        // 存在，写入redis
        this.set(key, r, time, unit);
        // 返回
        return r;
    }


    // 线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    /**
     * 逻辑过期的方式解决缓存击穿
     * @param id 商铺id
     * @return 商铺实体
     */
    public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {

        String key = keyPrefix + id;
        // 从 redis 查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 判断是否存在
        if (StrUtil.isBlank(json)) {
            // 未命中，直接返回
            return null;
        }
        // 命中 -> 过期与否都先把数据返回
        RedisData redisData = new RedisData();
        redisData = JSONUtil.toBean(json,RedisData.class);
        System.out.println(redisData.getData());
        JSONObject data = (JSONObject) redisData.getData();
        R r = JSONUtil.toBean(data, type);
        LocalDateTime expireTime = redisData.getExpireTime();

        // 判断是否过期

        // 过期时间在当前时间之后 -> 未过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            return r;
        }
        // 已过期
        // 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        if (isLock) {
            // 获取锁成功，开启独立线程，重建缓存
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 重建缓存
                    this.setWithLogicalExpire(key, dbFallback.apply(id), time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放锁
                    unlock(lockKey);
                }
            });
        }
        // 返回
        return r;
    }
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }
    private void unlock(String key){
        stringRedisTemplate.delete(key);

    }


}

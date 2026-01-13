package com.hmdp.utils;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {

    private StringRedisTemplate stringRedisTemplate;


    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time,unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){

        //设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));

        //写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));

    }

    //存空值""解决缓存穿透问题方法
    public <R,ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type,
            Function<ID,R> dbFallback, Long time, TimeUnit unit){
        String key = keyPrefix + id;

        //1.从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);

        //2.判断是否存在
        if (StrUtil.isNotBlank(json)) {
            //3.存在，直接返回
            return JSONUtil.toBean(json,type);
        }

        //判断命中的是否是空值，
        // 走到这里说明前面的isNotBlank为false,
        // 也就是说是blank，就是shopJson只可能是null或者""
        if(json != null){
            //不是null,说明shopJson只可能是""
            //也就是说查到的缓存是我们提前存好的""，
            //就直接返回null即可
            return null;
        }

        //4.不存在，根据id查询数据库
        R r = dbFallback.apply(id);

        //5.不存在，返回错误
        if(r == null){
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
            //返回错误信息
            return null;
        }

        //6.存在，写入redis
        this.set(key,r,time,unit);

        //7.返回
        return r;
    }


}

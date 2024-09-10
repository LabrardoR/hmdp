package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_LIST;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        String key = CACHE_SHOP_TYPE_LIST;
        // 从redis查询商铺类型缓存
        String shopTypeJson = stringRedisTemplate.opsForValue().get(key);
        // 缓存命中
        if(StrUtil.isNotBlank(shopTypeJson)){
            List<ShopType> list = JSONUtil.toList(shopTypeJson, ShopType.class);
            return Result.ok(list);
        }
        // 未命中，去数据库中查询并存入redis
        List<ShopType> list = query().orderByAsc("sort").list();
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(list));
        return Result.ok(list);
    }
}

-- 1.参数列表
-- 1.1 优惠券id
local voucherId = ARGV[1]
-- 1.2. 用户id
local userId = ARGV[2]
-- 1.3 订单id
local orderId = ARGV[3]

-- 2.数据key
-- 2.1 库存key (修复：去掉了冒号后的空格)
local stockKey = 'seckill:stock:' .. voucherId
-- 2.2 订单key (修复：变量名改为 orderKey，并去掉了空格)
local orderKey = 'seckill:order:' .. voucherId

-- 3 脚本业务
-- 3.1 判断库存是否充足
-- (修复：先 get 数据，再 tonumber 转换，最后再和 0 比较)
local stock = redis.call('get', stockKey)
if (tonumber(stock) <= 0) then
    -- 库存不足，返回1
    return 1
end

-- 3.2 判断用户是否重复下单
if (redis.call('sismember', orderKey, userId) == 1) then
    -- 3.3 存在说明是重复下单，返回2
    return 2
end

-- 3.4 扣减库存
redis.call('incrby', stockKey, -1)

-- 3.5 添加订单(保存用户)
redis.call('sadd', orderKey, userId)

--3.6 发送消息到队列中， XADD stream.orders * k1 v1 k2 v2 ...
redis.call('xadd','stream.orders','*','userId',userId,'voucherId',voucherId,'orderId',orderId)

-- 3.7 下单成功，返回0
return 0
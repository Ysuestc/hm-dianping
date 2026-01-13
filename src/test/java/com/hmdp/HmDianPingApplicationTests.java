package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private ShopServiceImpl shopService;

    @Resource
    private RedisIdWorker redisIdWorker;

    /*@Test
    void testSaveShop() throws InterruptedException {
        shopService.saveShop2Redis(1L,10L);
    }*/

    @Test
    void testSaveShop() throws InterruptedException {
        // 模拟管理员提前把 id=1 的店铺存入 Redis
        // 这里的 1L 是指逻辑过期时间为 1秒后（为了让你立刻测出过期效果）
        shopService.saveShop2Redis(1L, 1L);
        System.out.println("数据预热成功！");
    }

    //创建线程池
    private ExecutorService es = Executors.newFixedThreadPool(500);

    @Test
    void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            // 【关键修改】任务做完了，必须告诉 latch 计数减一
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }

}

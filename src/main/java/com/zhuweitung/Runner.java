package com.zhuweitung;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.cron.CronUtil;
import com.zhuweitung.model.Config;
import com.zhuweitung.utils.AreaUtils;
import com.zhuweitung.utils.ConfigUtils;
import com.zhuweitung.utils.SkuUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 运行类
 *
 * @author zhuweitung
 * @since 2024/10/17
 */
@Slf4j
public class Runner {

    @SneakyThrows
    public static void main(String[] args) {
        // 加载地区编码
        AreaUtils.load();

        // 获取配置
        Config config = ConfigUtils.get();
        List<String> skuIds = config.getSkuIds();
        if (CollUtil.isEmpty(skuIds)) {
            log.info("没有监控商品，请添加监控商品后重启");
            return;
        }
        Task task = new Task(skuIds);
        // 立即执行一次
        ThreadUtil.execute(task);
        CronUtil.schedule(config.getCron(), task);
        CronUtil.start();
    }

    private static class Task implements Runnable {

        private List<String> skuIds;
        private boolean running = false;

        public Task(List<String> skuIds) {
            this.skuIds = skuIds;
        }

        @Override
        public void run() {
            if (running) {
                return;
            }
            running = true;
            log.info("查询商品库存：{}", skuIds);
            SkuUtils.queryStock(skuIds);
            running = false;
        }
    }
}

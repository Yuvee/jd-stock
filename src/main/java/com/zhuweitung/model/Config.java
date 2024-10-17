package com.zhuweitung.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置信息
 *
 * @author zhuweitung
 * @since 2024/10/17
 */
@Data
public class Config {

    /**
     * 最小查询延迟
     */
    public static final int MIN_DELAY = 200;

    /**
     * 定时任务表达式（分钟级）
     */
    private String cron = "*/5 * * * *";

    /**
     * 监控商品ids
     */
    private List<String> skuIds = new ArrayList<>();

    /**
     * 每次查询延迟（毫秒）
     */
    private int delay = MIN_DELAY;

    /**
     * 启用通知
     */
    private boolean enableNotify;

    /**
     * 库存省份，省份有货后通知
     */
    private List<String> notifyProvinces = new ArrayList<>();

    /**
     * 钉钉机器人通知
     */
    private DingtalkBot dingtalkBot;

    @Data
    public static class DingtalkBot {
        private String token;
        private String secret;
    }

}

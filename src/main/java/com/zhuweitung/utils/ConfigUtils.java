package com.zhuweitung.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.setting.yaml.YamlUtil;
import com.zhuweitung.model.Config;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 配置信息工具类
 *
 * @author zhuweitung
 * @since 2024/10/17
 */
@Slf4j
public class ConfigUtils {

    private static Config CONFIG;

    static {
        load();
        print();
    }

    /**
     * 加载配置信息
     */
    private static void load() {
        File file = new File("data/config.yaml");
        if (FileUtil.exist(file)) {
            CONFIG = YamlUtil.load(FileUtil.getInputStream(file), Config.class);
            log.info("读取到配置文件");
        } else {
            // 默认配置
            CONFIG = new Config();
            log.info("未找到配置文件，使用默认配置");
            // 保存到配置文件
            YamlUtil.dump(CONFIG, FileUtil.getWriter(file, StandardCharsets.UTF_8, false));
        }
    }

    /**
     * 打印配置
     */
    public static void print() {
        log.info("=====配置信息=====");
        log.info("cron表达式：{}", CONFIG.getCron());
        log.info("监控商品：{}", CONFIG.getSkuIds());
        log.info("查询延迟：{}毫秒", CONFIG.getDelay());
        log.info("启用通知：{}", CONFIG.isEnableNotify());
        if (CONFIG.isEnableNotify()) {
            log.info("库存省份：{}", CONFIG.getNotifyProvinces());
            log.info("钉钉机器人配置：{}", JSONUtil.toJsonStr(CONFIG.getDingtalkBot()));
        }
        log.info("================");
    }

    /**
     * 获取配置
     *
     * @return 配置信息
     */
    public static Config get() {
        return CONFIG;
    }

    /**
     * 获取查询延迟（毫秒）
     *
     * @return 查询延迟（毫秒）
     */
    public static int getDelay() {
        return Math.max(Config.MIN_DELAY, CONFIG.getDelay());
    }

}

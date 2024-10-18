package com.zhuweitung.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.zhuweitung.model.Config;
import com.zhuweitung.model.SkuInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 商品查询工具类
 *
 * @author zhuweitung
 * @date 2024/10/16
 */
@Slf4j
public class SkuUtils {

    // 从接口返回结果中获取json数据的正则表达式
    private final static Pattern JSON_PATTERN = Pattern.compile("\\((\\{.*\\})\\)");

    /**
     * 查询库存
     *
     * @param skuIds 商品ids
     */
    public static void queryStock(List<String> skuIds) {
        Config config = ConfigUtils.get();

        Map<String, Object> params = new HashMap<>();
        params.put("type", "getstocks");
        params.put("skuIds", StrUtil.join(",", skuIds));
        params.put("appid", "item-v3");
        params.put("functionId", "pc_stocks");
        params.put("callback", "jQuery111107584463972365898_1729065548044");

        int delay = ConfigUtils.getDelay();
        List<String> provinces = config.getProvinces();
        List<String> areaCodes = AreaUtils.getRandomCodeCombination(provinces);
        Map<String, List<String>> stockAreaNames = new HashMap<>();
        for (String areaCode : areaCodes) {
            params.put("area", areaCode);
            params.put("_", System.currentTimeMillis());
            String response = HttpRequest.get("https://api.m.jd.com/stocks")
                    .header("user-agent", config.getUa())
                    .form(params)
                    .execute().body();
            List<String> groups = ReUtil.findAllGroup1(JSON_PATTERN, response);
            String areaName = AreaUtils.getAreaName(StrUtil.split(areaCode, "_").get(0));

            try {
                for (String skuId : skuIds) {
                    SkuInfo skuInfo = null;
                    skuInfo = JSONUtil.toBean(JSONUtil.parseObj(groups.get(0)).getStr(skuId), SkuInfo.class);
                    String stockStateName = Optional.ofNullable(skuInfo).map(SkuInfo::getStockStateName).orElse("未知");
                    log.info("[{}] {}：{}", skuId, areaName, stockStateName);
                    if (Objects.equals(stockStateName, "现货")) {
                        List<String> list = stockAreaNames.getOrDefault(skuId, new ArrayList<>());
                        list.add(areaName);
                        stockAreaNames.put(skuId, list);
                    }
                }
            } catch (Exception e) {
                log.error("{}：查询异常，response={}", areaName, response);
            }

            ThreadUtil.sleep(delay);
        }

        for (String skuId : skuIds) {
            List<String> areaNames = stockAreaNames.get(skuId);
            List<String> messages = new ArrayList<>();
            if (CollUtil.isNotEmpty(areaNames)) {
                Collection<String> intersection = CollUtil.intersection(provinces, areaNames);
                if (CollUtil.isEmpty(provinces)) {
                    // 未配置通知省份则有现货的省份都通知
                    messages.add(StrUtil.format("商品 {} 在 {} 地区有现货！", skuId, StrUtil.join("、", areaNames)));
                } else if (CollUtil.isNotEmpty(intersection)) {
                    // 或者有现货的省份在配置中则通知
                    messages.add(StrUtil.format("商品 {} 在 {} 地区有现货！", skuId, StrUtil.join("、", intersection)));
                }
            }
            MessageUtils.send(StrUtil.join("\n", messages));
        }

    }

}

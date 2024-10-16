import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
     * @param skuId 商品id
     */
    public static void queryStock(String skuId) {
        Map<String, Object> params = new HashMap<>();
        params.put("type", "getstocks");
        params.put("skuIds", skuId);
        params.put("appid", "item-v3");
        params.put("functionId", "pc_stocks");
        params.put("callback", "jQuery111107584463972365898_1729065548044");

        List<String> areaCodes = AreaUtils.getRandomCodeCombination();
        for (String areaCode : areaCodes) {
            params.put("area", areaCode);
            params.put("_", System.currentTimeMillis());
            String response = HttpUtil.get("https://api.m.jd.com/stocks", params);
            List<String> groups = ReUtil.findAllGroup1(JSON_PATTERN, response);
            SkuInfo skuInfo = JSONUtil.toBean(JSONUtil.parseObj(groups.get(0)).getStr(skuId), SkuInfo.class);
            log.info("{}：{}", AreaUtils.getAreaName(StrUtil.split(areaCode, "_").get(0)),
                    Optional.ofNullable(skuInfo).map(SkuInfo::getStockStateName).orElse("未知"));
        }
    }

}

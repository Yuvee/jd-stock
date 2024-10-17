package com.zhuweitung.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.zhuweitung.model.AreaInfo;
import com.zhuweitung.model.Config;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * 京东地区编码查询
 *
 * @author zhuweitung
 * @since 2024/10/16
 */
@Slf4j
public class AreaUtils {

    // 省级编码
    private final static List<AreaInfo> PROVINCES = JSONUtil.toList("[{\"id\":\"2\",\"name\":\"上海\"},{\"id\":\"12\",\"name\":\"江苏\"},{\"id\":\"15\",\"name\":\"浙江\"},{\"id\":\"14\",\"name\":\"安徽\"},{\"id\":\"19\",\"name\":\"广东\"},{\"id\":\"20\",\"name\":\"广西\"},{\"id\":\"16\",\"name\":\"福建\"},{\"id\":\"23\",\"name\":\"海南\"},{\"id\":\"1\",\"name\":\"北京\"},{\"id\":\"5\",\"name\":\"河北\"},{\"id\":\"11\",\"name\":\"内蒙古\"},{\"id\":\"13\",\"name\":\"山东\"},{\"id\":\"6\",\"name\":\"山西\"},{\"id\":\"3\",\"name\":\"天津\"},{\"id\":\"17\",\"name\":\"湖北\"},{\"id\":\"18\",\"name\":\"湖南\"},{\"id\":\"7\",\"name\":\"河南\"},{\"id\":\"21\",\"name\":\"江西\"},{\"id\":\"8\",\"name\":\"辽宁\"},{\"id\":\"10\",\"name\":\"黑龙江\"},{\"id\":\"9\",\"name\":\"吉林\"},{\"id\":\"22\",\"name\":\"四川\"},{\"id\":\"4\",\"name\":\"重庆\"},{\"id\":\"25\",\"name\":\"云南\"},{\"id\":\"24\",\"name\":\"贵州\"},{\"id\":\"26\",\"name\":\"西藏\"},{\"id\":\"27\",\"name\":\"陕西\"},{\"id\":\"30\",\"name\":\"宁夏\"},{\"id\":\"28\",\"name\":\"甘肃\"},{\"id\":\"29\",\"name\":\"青海\"},{\"id\":\"31\",\"name\":\"新疆\"}]",
            AreaInfo.class);

    // 从接口返回结果中获取json数据的正则表达式
    private final static Pattern JSON_PATTERN = Pattern.compile("\\((\\[.*\\])\\)");

    private final static List<AreaInfo> AREAS = new ArrayList<>();
    private static Tree<String> AREA_TREE;

    /**
     * 加载京东地区编码
     */
    public static void load() {
        List<AreaInfo> areas = fetch();
        AREAS.clear();
        AREAS.addAll(areas);
        // 构建树形结构
        AREA_TREE = TreeUtil.buildSingle(areas, "0",
                (obj, treeNode) -> {
                    treeNode.setId(obj.getId());
                    treeNode.setParentId(obj.getPid());
                    treeNode.setWeight(obj.getLevel());
                    treeNode.setName(obj.getName());
                });
    }

    /**
     * 获取京东地区编码
     *
     * @return 京东地区编码列表
     */
    @SneakyThrows
    private static List<AreaInfo> fetch() {
        File file = new File("data/area_code.json");
        if (FileUtil.exist(file)) {
            log.info("存在地区编码数据，执行读取");
            List<AreaInfo> areas = JSONUtil.toList(FileUtil.readUtf8String(file), AreaInfo.class);
            if (CollUtil.isNotEmpty(areas)) {
                return areas;
            }
        }
        log.info("不存在地区编码数据，执行获取");
        FileUtil.mkParentDirs(file);
        List<AreaInfo> areas = new CopyOnWriteArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch countDownLatch = new CountDownLatch(PROVINCES.size());
        for (AreaInfo area : PROVINCES) {
            pool.submit(() -> {
                try {
                    areas.add(area);
                    fetchNext(area, areas, 0);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
        pool.shutdown();
        // 写入文件
        FileUtil.writeUtf8String(JSONUtil.toJsonStr(areas), file);
        return areas;
    }

    /**
     * 获取下级地区编码
     *
     * @param current 当前地区
     * @param areas   地区列表
     * @param level   层级
     */
    private static void fetchNext(AreaInfo current, List<AreaInfo> areas, int level) {
        if (level >= 3) {
            return;
        }
        Config config = ConfigUtils.get();
        Map<String, Object> params = new HashMap<>();
        params.put("fid", current.getId());
        params.put("callback", "jQuery1111047169012038874314_1729066415663");
        params.put("_", System.currentTimeMillis());
        log.debug("查询地区编码：{} {}", current.getName(), current.getId());
        String response = HttpRequest.get("https://fts.jd.com/area/get")
                .header("user-agent", config.getUa())
                .form(params)
                .execute().body();
        List<String> groups = ReUtil.findAllGroup1(JSON_PATTERN, response);
        List<AreaInfo> nextAreas = JSONUtil.toList(groups.get(0), AreaInfo.class);
        if (CollUtil.isEmpty(nextAreas)) {
            return;
        }
        for (AreaInfo area : nextAreas) {
            area.setPid(current.getId());
            area.setLevel(level + 1);
            areas.add(area);
            fetchNext(area, areas, level + 1);
        }
    }

    /**
     * 获取随机地区编码组合（各省）
     *
     * @param provinceNames 省份名称列表
     * @return 各省地区编码 0_0_0_0
     */
    public static List<String> getRandomCodeCombination(List<String> provinceNames) {
        List<String> codes = new ArrayList<>();
        for (AreaInfo province : PROVINCES) {
            if (CollUtil.isNotEmpty(provinceNames) && !provinceNames.contains(province.getName())) {
                continue;
            }
            Tree<String> node = TreeUtil.getNode(AREA_TREE, province.getId());
            List<String> nodeCodes = getRandomChildrenCode(node);
            if (nodeCodes.size() < 4) {
                for (int i = 0; i < 4 - nodeCodes.size(); i++) {
                    nodeCodes.add("0");
                }
            }
            codes.add(StrUtil.join("_", nodeCodes));
        }
        return codes;
    }

    /**
     * 获取随机子节点编码
     *
     * @param node 树节点
     * @return 编码列表
     */
    private static List<String> getRandomChildrenCode(Tree<String> node) {
        List<String> codes = new ArrayList<>();
        codes.add(node.getId());
        List<Tree<String>> children = node.getChildren();
        if (CollUtil.isEmpty(children)) {
            return codes;
        }
        Tree<String> randomChild = RandomUtil.randomEle(children);
        CollUtil.addAll(codes, getRandomChildrenCode(randomChild));
        return codes;
    }

    /**
     * 获取地区名称
     *
     * @param id 地区编码
     * @return 地区名称
     */
    public static String getAreaName(String id) {
        Tree<String> node = TreeUtil.getNode(AREA_TREE, id);
        return (String) Optional.ofNullable(node).map(Tree::getName).orElse(null);
    }


}

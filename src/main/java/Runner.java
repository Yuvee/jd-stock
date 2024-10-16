import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class Runner {

    // 省级编码
    private final static List<AreaInfo> AREA_INFOS = JSONUtil.toList("[{\"id\":\"2\",\"name\":\"上海\"},{\"id\":\"12\",\"name\":\"江苏\"},{\"id\":\"15\",\"name\":\"浙江\"},{\"id\":\"14\",\"name\":\"安徽\"},{\"id\":\"19\",\"name\":\"广东\"},{\"id\":\"20\",\"name\":\"广西\"},{\"id\":\"16\",\"name\":\"福建\"},{\"id\":\"23\",\"name\":\"海南\"},{\"id\":\"1\",\"name\":\"北京\"},{\"id\":\"5\",\"name\":\"河北\"},{\"id\":\"11\",\"name\":\"内蒙古\"},{\"id\":\"13\",\"name\":\"山东\"},{\"id\":\"6\",\"name\":\"山西\"},{\"id\":\"3\",\"name\":\"天津\"},{\"id\":\"17\",\"name\":\"湖北\"},{\"id\":\"18\",\"name\":\"湖南\"},{\"id\":\"7\",\"name\":\"河南\"},{\"id\":\"21\",\"name\":\"江西\"},{\"id\":\"8\",\"name\":\"辽宁\"},{\"id\":\"10\",\"name\":\"黑龙江\"},{\"id\":\"9\",\"name\":\"吉林\"},{\"id\":\"22\",\"name\":\"四川\"},{\"id\":\"4\",\"name\":\"重庆\"},{\"id\":\"25\",\"name\":\"云南\"},{\"id\":\"24\",\"name\":\"贵州\"},{\"id\":\"26\",\"name\":\"西藏\"},{\"id\":\"27\",\"name\":\"陕西\"},{\"id\":\"30\",\"name\":\"宁夏\"},{\"id\":\"28\",\"name\":\"甘肃\"},{\"id\":\"29\",\"name\":\"青海\"},{\"id\":\"31\",\"name\":\"新疆\"}]",
            AreaInfo.class);

    private final static Pattern JSON_PATTERN = Pattern.compile("\\((\\[.*\\])\\)");

    @SneakyThrows
    public static void main(String[] args) {
        List<AreaInfo> areas = new CopyOnWriteArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch countDownLatch = new CountDownLatch(AREA_INFOS.size());
        for (AreaInfo area : AREA_INFOS) {
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
        FileUtil.writeUtf8String(JSONUtil.toJsonStr(areas), new File("area_code.json"));
    }

    /**
     * 获取下级地区编码
     *
     * @param current 当前地区
     * @param areas 地区列表
     * @param level 层级
     */
    public static void fetchNext(AreaInfo current, List<AreaInfo> areas, int level) {
        if (level >= 3) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("fid", current.getId());
        params.put("callback", "jQuery1111047169012038874314_1729066415663");
        params.put("_", System.currentTimeMillis());
        log.info("fetch fid:{}", current.getId());
        String response = HttpUtil.get("https://fts.jd.com/area/get", params);
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


}

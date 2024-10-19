package utils

import (
	"encoding/json"
	"fmt"
	"github.com/zhuweitung/jd-stock/models"
	"golang.org/x/text/encoding/simplifiedchinese"
	"golang.org/x/text/transform"
	"io"
	"log"
	"net/http"
	"regexp"
	"strings"
	"time"
)

var (
	// 正则表达式，用于提取 JSON 数据部分
	skuJsonPattern = regexp.MustCompile(`\((\{.*})\)`)
)

// QueryStock 查询库存
func QueryStock(customSkuInfos []models.CustomSkuInfo) {
	config := GetConfig()

	provinceNames := config.Provinces
	areaCodeCombinations := GetRandomCodeCombination(provinceNames)
	stockAreaNames := make(map[string][]string)

	client := &http.Client{Timeout: 10 * time.Second}

	req, err := http.NewRequest("GET", "https://api.m.jd.com/stocks", nil)
	if err != nil {
		log.Printf("请求创建失败: %v", err)
		return
	}

	for index, areaCodeCombination := range areaCodeCombinations {
		q := req.URL.Query()
		q.Add("type", "getstocks")
		q.Add("skuIds", getSkuIds(customSkuInfos))
		q.Add("appid", "item-v3")
		q.Add("functionId", "pc_stocks")
		q.Add("callback", "jQuery111107584463972365898_1729065548044")
		q.Add("area", areaCodeCombination)
		q.Add("_", fmt.Sprint(time.Now().UnixMilli()))
		req.URL.RawQuery = q.Encode()
		req.Header.Set("User-Agent", GetConfig().Ua)

		// 发送请求
		resp, _ := client.Do(req)
		defer resp.Body.Close()

		body, _ := io.ReadAll(resp.Body)
		contentType := resp.Header.Get("Content-Type")
		var response string
		if strings.Contains(strings.ToLower(contentType), "gbk") {
			response, _ = convertGBKToUTF8(body)
		} else {
			response = string(body)
		}
		groups := skuJsonPattern.FindStringSubmatch(response)
		area, err := GetAreaByID(strings.Split(areaCodeCombination, "_")[0])
		if err != nil {
			log.Printf("%v", err)
			return
		}

		if len(groups) > 1 {
			var skuInfoMap map[string]models.SkuInfo
			if err := json.Unmarshal([]byte(groups[1]), &skuInfoMap); err != nil {
				log.Printf("%s：查询异常，response=%s", area.Name, response)
				continue
			}

			for _, customSkuInfo := range customSkuInfos {
				skuId := customSkuInfo.Id
				skuInfo, ok := skuInfoMap[skuId]
				if !ok {
					continue
				}
				stockStateName := skuInfo.StockStateName
				log.Printf("[%s] %s %s：%s", skuId, customSkuInfo.Name, area.Name, stockStateName)

				if stockStateName == "现货" {
					stockAreaNames[skuId] = append(stockAreaNames[skuId], area.Name)
				}
			}
		} else {
			log.Printf("%s：查询异常，response=%s", area.Name, response)
		}

		if index != len(areaCodeCombinations)-1 {
			time.Sleep(time.Duration(GetDelay()) * time.Millisecond)
		}
	}

	var messages []string
	for _, customSkuInfo := range customSkuInfos {
		skuId := customSkuInfo.Id
		areaNames := stockAreaNames[skuId]
		intersection := getIntersection(provinceNames, areaNames)
		if len(provinceNames) == 0 {
			messages = append(messages, fmt.Sprintf("商品 [%s] %s 在 %s 地区有现货！\n", skuId, customSkuInfo.Name, strings.Join(areaNames, "、")))
		} else if len(intersection) > 0 {
			messages = append(messages, fmt.Sprintf("商品 [%s] %s 在 %s 地区有现货！\n", skuId, customSkuInfo.Name, strings.Join(intersection, "、")))
		}
	}
	if len(messages) > 0 {
		message := strings.Join(messages, "\n")
		log.Printf("%s", message)
		SendMessage(message)
	} else {
		log.Printf("商品 %v 无货...", customSkuInfos)
	}
}

// 获取商品ids
func getSkuIds(skuInfos []models.CustomSkuInfo) string {
	if len(skuInfos) == 0 {
		return ""
	}
	var skuIds []string
	for _, skuInfo := range skuInfos {
		skuIds = append(skuIds, skuInfo.Id)
	}
	return strings.Join(skuIds, ",")
}

// 获取两个字符串数组的交集
func getIntersection(arr1, arr2 []string) []string {
	// 创建一个映射用于存储数组元素
	set := make(map[string]struct{})
	for _, item := range arr1 {
		set[item] = struct{}{} // 使用空结构体占位，节省内存
	}
	var intersection []string
	for _, item := range arr2 {
		if _, exists := set[item]; exists {
			intersection = append(intersection, item) // 如果存在于第一个数组中，则加入交集
		}
	}
	return intersection
}

// 将 GBK 编码的字节数组转换为 UTF-8 字符串
func convertGBKToUTF8(gbkData []byte) (string, error) {
	reader := transform.NewReader(strings.NewReader(string(gbkData)), simplifiedchinese.GBK.NewDecoder())
	utf8Data, err := io.ReadAll(reader)
	if err != nil {
		return "", err
	}
	return string(utf8Data), nil
}

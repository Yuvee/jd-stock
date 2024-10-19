package utils

import (
	"crypto/md5"
	"encoding/hex"
	"encoding/json"
	"github.com/zhuweitung/jd-stock/models"
	"log"
	"os"
	"time"
)

var (
	// 通知缓存文件路径
	messageCacheFilepath = "config/message_cache.json"
)

// SendMessage 发送通知
func SendMessage(msg string) {
	config := GetConfig()
	if !config.EnableNotify {
		// 未开启通知，跳过
		return
	}
	if hasCache(msg) {
		// 存在通知缓存，跳过
		return
	}
	// 获取通知消息发送客户端
	sender, err := GetSender()
	if err != nil {
		log.Printf("%v", err)
		return
	}
	err = sender.Send(msg)
	if err == nil {
		appendCache(msg)
	} else {
		log.Printf("%v，跳过通知\n", err)
	}
}

// 获取缓存
func getCaches() []models.MessageCache {
	data, err := os.ReadFile(messageCacheFilepath)
	if err != nil {
		return nil
	}
	var caches []models.MessageCache
	_ = json.Unmarshal(data, &caches)
	return caches
}

// 是否有缓存
func hasCache(msg string) bool {
	notifyInterval := GetConfig().NotifyInterval
	if notifyInterval == 0 {
		return false
	}
	caches := getCaches()
	if caches == nil {
		return false
	}
	md5HexStr := calculateMd5(msg)
	now, _ := time.Parse("2006-01-02 15:04:05", time.Now().Format("2006-01-02 15:04:05"))
	for _, cache := range caches {
		if md5HexStr == cache.Content {
			// 判断时间间隔是否小于通知间隔
			t, _ := time.Parse("2006-01-02 15:04:05", cache.Time)
			if diffMinutes(now, t) < notifyInterval {
				// 不通知
				return true
			} else {
				// 超过间隔时间，删除通知缓存
				caches = removeCache(caches, cache)
				saveCacheFile(caches)
				return false
			}
		}
	}
	return false
}

// 删除缓存
func removeCache(arr []models.MessageCache, target models.MessageCache) []models.MessageCache {
	var result []models.MessageCache
	for _, p := range arr {
		if p != target {
			result = append(result, p)
		}
	}
	return result
}

// 追加缓存
func appendCache(msg string) {
	caches := getCaches()
	caches = append(caches, models.MessageCache{Content: calculateMd5(msg), Time: time.Now().Format("2006-01-02 15:04:05")})
	saveCacheFile(caches)
}

// 保存到文件
func saveCacheFile(caches []models.MessageCache) {
	data, _ := json.Marshal(caches)
	_ = os.WriteFile(messageCacheFilepath, data, 0644)
}

// 计算md5值
func calculateMd5(msg string) string {
	hash := md5.New()
	hash.Write([]byte(msg))
	return hex.EncodeToString(hash.Sum(nil))
}

// 时间差（分钟）
func diffMinutes(t1, t2 time.Time) int {
	duration := t2.Sub(t1)
	return absInt(int(duration.Minutes()))
}

// 计算整数的绝对值
func absInt(n int) int {
	if n < 0 {
		return -n
	}
	return n
}
